package com.kote.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior}

import scala.concurrent.duration.DurationInt

object ScrapperManagerActor {

  val FetchStateTimer = 5.seconds

  case class UrlSpecification(providerId: Int, familyId: Int, url: String, categories: List[Int], lastUrlScrapped: Option[String] = None)

  sealed trait Command

  final case class AddUrl(urlSpecification: UrlSpecification) extends Command

  final case object FetchState extends Command

  sealed trait Event

  final case class UrlAdded(urlSpecification: UrlSpecification) extends Event

  final case class State(specifications: Map[Int, List[UrlSpecification]])

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timer =>
      timer.startSingleTimer(FetchState, FetchStateTimer)

      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId("scrapperManager"),
        emptyState = State(Map.empty),
        commandHandler = commandHandler(context, timer),
        eventHandler = eventHandler(context)
      )
    }
  }

  def commandHandler(context: ActorContext[Command], timer: TimerScheduler[Command])(state: State, cmd: Command): EffectBuilder[Event, State] =
    cmd match {
      case AddUrl(urlSpecification) =>
        if (specificationAlreadyExists(urlSpecification, state)) {
          context.log.info("Cannot add an already known specification, command rejected")
          Effect.none
        } else
          Effect.persist(UrlAdded(urlSpecification))

      case FetchState =>
        context.log.info(s"FetchState command received, state=$state")
        timer.startSingleTimer(FetchState, FetchStateTimer)
        Effect.none
    }

  def specificationAlreadyExists(urlSpecification: UrlSpecification, state: State): Boolean = {
    state
      .specifications
      .get(urlSpecification.providerId)
      .fold(false)(_.contains(urlSpecification))
  }

  def eventHandler(context: ActorContext[Command])(state: State, event: Event): State =
    event match {
      case UrlAdded(urlSpecification) =>
        val newSpecifications: Map[Int, List[UrlSpecification]] =
          state
            .specifications
            .get(urlSpecification.providerId)
            .fold(state.specifications + (urlSpecification.providerId -> List(urlSpecification))) { specs =>
              state.specifications + (urlSpecification.providerId -> (specs :+ urlSpecification))
            }
        state.copy(specifications = newSpecifications)
    }
}

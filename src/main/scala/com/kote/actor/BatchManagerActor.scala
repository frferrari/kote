package com.kote.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior}
import com.kote.model.BatchSpecification

import scala.concurrent.duration.DurationInt

object BatchManagerActor {

  val FetchStateTimer = 5.seconds

  sealed trait Command

  final case class AddUrl(batchSpecification: BatchSpecification) extends Command

  final case object FetchState extends Command

  sealed trait Event

  final case class UrlAdded(batchSpecification: BatchSpecification) extends Event

  final case class State(specifications: List[BatchSpecification])

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timer =>
      timer.startSingleTimer(FetchState, FetchStateTimer)

      val batchProcessorActor: ActorRef[BatchProcessorActor.Command] =
        context.spawn(BatchProcessorActor(), "batchProcessor")

      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId("batchManager"),
        emptyState = State(Nil),
        commandHandler = commandHandler(context, timer, batchProcessorActor),
        eventHandler = eventHandler(context)
      )
    }
  }

  def commandHandler(context: ActorContext[Command],
                     timer: TimerScheduler[Command],
                     batchProcessor: ActorRef[BatchProcessorActor.Command])
                    (state: State, cmd: Command): EffectBuilder[Event, State] =
    cmd match {
      case AddUrl(batchSpecification) =>
        if (specificationAlreadyExists(batchSpecification, state)) {
          context.log.info("Cannot add an already known specification, command rejected")
          Effect.none
        } else
          Effect
            .persist(UrlAdded(batchSpecification))
            .thenRun(_ => batchProcessor ! BatchProcessorActor.ScrapUrl("thisUrl"))

      case FetchState =>
        context.log.info(s"FetchState command received, state=$state")
        timer.startSingleTimer(FetchState, FetchStateTimer)
        Effect.none
    }

  def specificationAlreadyExists(batchSpecification: BatchSpecification, state: State): Boolean =
    state
      .specifications
      .contains(batchSpecification)

  def eventHandler(context: ActorContext[Command])(state: State, event: Event): State =
    event match {
      case UrlAdded(urlSpecification) =>
        val newSpecifications: List[BatchSpecification] = state.specifications :+ urlSpecification
        state.copy(specifications = newSpecifications)
    }
}

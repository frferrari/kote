package com.kote.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink

object BatchActor {

  sealed trait Command

  final case class AddAuction(auctionId: String) extends Command

  sealed trait Event

  final case class AuctionAdded(auctionId: String) extends Event

  case class State(auctionIds: List[String])

  def apply(actorId: String): Behavior[Command] = Behaviors.setup { context =>

    context.log.info(s"Batch actor creation actorId=$actorId")

    (1 to 4).foreach(idx => context.self ! AddAuction(s"AuctionId$idx"))

    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.of("batch", actorId),
      emptyState = State(Nil),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )
  }

  def commandHandler(context: ActorContext[Command])(state: State, cmd: Command): EffectBuilder[Event, State] = {
    cmd match {
      case AddAuction(auctionId) =>
        Effect.persist(AuctionAdded(auctionId)).thenRun(_ => recoverChildren(context))
    }
  }

  def eventHandler(context: ActorContext[Command])(state: State, event: Event): State = {
    event match {
      case AuctionAdded(auctionId) =>
        context.log.info(s"BatchActor state=$state event=$event")
        state.copy(auctionIds = state.auctionIds :+ auctionId)
    }
  }

  private def recoverChildren(context: ActorContext[Command]): Unit = {
    // https://doc.akka.io/docs/akka-persistence-jdbc/current/
    // https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html
    implicit val mat: Materializer = Materializer(context)

    val readJournal =
      PersistenceQuery(context.system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

    context.log.info(s"===========> BatchActor(recoverChildren) ${readJournal.toString}")

    val printlnSink = Sink.foreach { p: String => context.log.info(s"BatchActor(recoverChildren) persistenceId $p") }

    readJournal
      .currentPersistenceIds()
      .runWith(printlnSink)

    ()
//    readJournal
//      .persistenceIds()
//      .runForeach { persistenceId =>
//        context.log.info(s"BatchActor(recoverChildren) persistenceId $persistenceId")
//      }
  }
}

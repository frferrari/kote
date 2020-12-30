package com.kote.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer

object BatchProcessorActor {

  sealed trait Command
  final case class ScrapUrl(url: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info(s"BatchProcessorActor creation")

    recoverChildren(context)

    Behaviors.receiveMessage {
      case ScrapUrl(url) =>
        val actorId = java.util.UUID.randomUUID().toString
        context.spawn(BatchActor(actorId), actorId)
        Behaviors.same
    }
  }

  private def recoverChildren(context: ActorContext[Command]) = {
    // https://doc.akka.io/docs/akka-persistence-jdbc/current/
    // https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html
    implicit val mat: Materializer = Materializer(context)

    val readJournal =
      PersistenceQuery(context.system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

    context.log.info(s"===========> BatchProcessorActor(recoverChildren) ${readJournal.toString}")

    readJournal
      .persistenceIds()
      .runForeach { persistenceId =>
        context.log.info(s"BatchProcessorActor(recoverChildren) persistenceId $persistenceId")
        // context.spawn(BatchActor(persistenceId), persistenceId)
      }
  }
}

package com.kote

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post, _}
import akka.http.scaladsl.server.Route
import com.kote.actor.BatchManagerActor
import com.kote.model.{BatchSpecification, BatchSpecificationJsonProtocol}

import scala.io.StdIn

object BatchApp
  extends BatchSpecificationJsonProtocol
    with SprayJsonSupport {
  implicit val actorSystem = ActorSystem(BatchManagerActor(), "batchManager")
  implicit val executionContext = actorSystem.executionContext

  val routes: Route =
    path("scrap") {
      post {
        entity(as[BatchSpecification]) { batchSpecification =>
          println(s"batchSpecification $batchSpecification")
          actorSystem ! BatchManagerActor.AddUrl(batchSpecification)
          complete(StatusCodes.OK)
        }
      }
    } ~ path("fetchState") {
        get {
          actorSystem ! BatchManagerActor.FetchState
          complete(StatusCodes.OK)
        }
      }

  def main(args: Array[String]): Unit = {
    val bindingFuture = Http().newServerAt("localhost", 8080).bind(routes)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine() // let it run until user presses return
    StdIn.readLine() // let it run until user presses return
    StdIn.readLine() // let it run until user presses return
    StdIn.readLine() // let it run until user presses return
    // Thread.sleep(60000);

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => actorSystem.terminate()) // and shutdown when done
  }
}

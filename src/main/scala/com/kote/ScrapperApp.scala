package com.kote

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post, _}
import akka.http.scaladsl.server.Route
import com.kote.actor.ScrapperManagerActor
import com.kote.actor.ScrapperManagerActor.UrlSpecification
import com.kote.model.{ScrapSpecification, ScrapSpecificationJsonProtocol}

import scala.io.StdIn

object ScrapperApp
  extends ScrapSpecificationJsonProtocol
    with SprayJsonSupport {
  implicit val actorSystem = ActorSystem(ScrapperManagerActor(), "scrapperManager")
  implicit val executionContext = actorSystem.executionContext

  val routes: Route =
    path("scrap") {
      post {
        entity(as[ScrapSpecification]) { scrapSpecification =>
          println(s"scrapSpecification $scrapSpecification")
          actorSystem ! ScrapperManagerActor.AddUrl(UrlSpecification(scrapSpecification.provider, scrapSpecification.familyId, scrapSpecification.url, scrapSpecification.categories))
          complete(StatusCodes.OK)
        }
      }
    } ~ path("fetchState") {
        get {
          actorSystem ! ScrapperManagerActor.FetchState
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

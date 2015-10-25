package org.bustos.vuelta

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.util.{Failure, Success}
import spray.can.Http
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Vuelta extends App {

  def doMain() {

    implicit val system = ActorSystem()
    implicit val timeout = Timeout(DurationInt(5).seconds)

    val server = system.actorOf(Props[VueltaServiceActor], "vueltaRoutes")
    val config = ConfigFactory.load

    if (args.length > 0) IO(Http) ? Http.Bind(server, "0.0.0.0", args(0).toInt)
    else IO(Http) ? Http.Bind(server, "0.0.0.0",  config.getInt("vueltaServer.port"))
  }

  doMain()
}

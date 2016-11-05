/*

    Copyright (C) 2015 Mauricio Bustos (m@bustos.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.bustos.vuelta

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.tototoshi.csv._
import com.typesafe.config.ConfigFactory
import org.joda.time.{DateTime, DateTimeZone}
import spray.can.Http

import scala.concurrent.duration._
import scala.util.Properties.envOrElse

object Vuelta extends App {

  def doMain = {

    implicit val system = ActorSystem()
    implicit val timeout = Timeout(DurationInt(5).seconds)

    val server = system.actorOf(Props[VueltaServiceActor], "vueltaRoutes")
    val config = ConfigFactory.load

    val port = envOrElse("PORT", config.getString("vueltaServer.port"))

    if (args.length > 0) IO(Http) ? Http.Bind(server, "0.0.0.0", args(0).toInt)
    else IO(Http) ? Http.Bind(server, "0.0.0.0", port.toInt)
  }

  def updateRiders = {
    import VueltaData._
    import VueltaTables._

    import scala.slick.driver.MySQLDriver.simple._

    db.withSession { implicit session =>
      riderTable.filter(_.bibNumber > 0).delete
      riderEventTable.filter(_.bibNumber > 0).delete
      val reader = CSVReader.open(new File("/Users/mauricio/Downloads/Final Rider List.csv"))
      reader.foreach(fields => {
        println(fields)
        if (fields(0) != "" && fields(0).forall(_.isDigit)) {
          riderTable += Rider(fields(0).toInt, fields(2) + " " + fields(1), new DateTime(DateTimeZone.UTC))
          val date = new DateTime(DateTimeZone.UTC)
          val localdate = new DateTime
          riderEventTable += RiderEvent(fields(0).toInt, RestStops(0).latitude, RestStops(0).longitude, date)
        }
      })
    }
  }

  //updateRiders
  doMain
}

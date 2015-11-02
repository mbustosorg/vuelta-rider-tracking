package org.bustos.vuelta

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import org.bustos.vuelta.VueltaTables.{RiderEvent, Rider}
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import scala.util.Properties.envOrElse
import scala.slick.driver.MySQLDriver.simple._
import org.joda.time._
import scala.concurrent.duration._
import spray.json._

object VueltaData {

  val quarterMile = 1.0 / 60.0 / 8.0 // In degrees

  val hhmmssFormatter = DateTimeFormat.forPattern("hh:mm:ss")

  val db = {
    //val mysqlURL = envOrElse("VUELTA_MYSQL_URL", "jdbc:mysql://localhost:3306/vuelta")
    //val mysqlUser = envOrElse("VUELTA_MYSQL_USER", "root") // vueltauser
    //val mysqlPassword = envOrElse("VUELTA_MYSQL_PASSWORD", "") // m6gs9yezyuqv
    val mysqlURL = envOrElse("VUELTA_MYSQL_URL", "jdbc:mysql://mysql.bustos.org:3306/vuelta")
    val mysqlUser = envOrElse("VUELTA_MYSQL_USER", "vueltauser") // vueltauser
    val mysqlPassword = envOrElse("VUELTA_MYSQL_PASSWORD", "m6gs9yezyuqv") // m6gs9yezyuqv
    Database.forURL(mysqlURL, driver = "com.mysql.jdbc.Driver", user = mysqlUser, password = mysqlPassword)
  }
}

class VueltaData extends Actor with ActorLogging {

  import VueltaData._
  import VueltaTables._
  import VueltaJsonProtocol._

  val logger =  LoggerFactory.getLogger(getClass)

  implicit val defaultTimeout = Timeout(1 seconds)

  var riders: Map[Int, Rider] = {
    val fullList = db.withSession { implicit session =>
      riderTable.list.map({ x => (x.bibNumber -> Rider(x.bibNumber, x.name, x.registrationDate))})
    }
    fullList.toMap
  }
  var riderEvents = Map.empty[Int, RiderEvent]

  def restStop(event: RiderEvent): RestStop = {
    RestStops.find({ x =>
      (x.latitude - event.latitude).abs < quarterMile && (x.longitude - event.longitude).abs < quarterMile
    }) match {
      case Some(stop) => stop
      case _ => OffCourse
    }
  }

  def riderCounts: List[(String, Int)] = {

    val stopsForEvents = db.withSession { implicit session =>
      latestEventPerRider.list
    }.map(restStop(_)).groupBy(_.name)

    RestStops.map { x =>
      if (stopsForEvents.contains(x.name)) (x.name, stopsForEvents(x.name).length)
      else (x.name, 0)
    }
  }

  def receive = {
    case RiderRequest(bibNumber) => {
      val rider = {
        if (riders.contains(bibNumber)) riders(bibNumber)
        else {
          val riders = db.withSession { implicit session =>
            riderTable.filter(_.bibNumber === bibNumber).list
          }
          if (riders.isEmpty) Rider(0, "", null)
          else riders.head
        }
      }
      sender ! rider
    }
    case RiderUpdate(bibNumber, latitude, longitude) =>
      val riderConfirm = {
        if (!riders.contains(bibNumber)) RiderConfirm(Rider(0, "", null), RiderEvent(0, 0.0, 0.0, new DateTime(DateTimeZone.UTC)))
        else {
          val event = RiderEvent(bibNumber, latitude, longitude, new DateTime(DateTimeZone.UTC))
          db.withSession { implicit session =>
            riderEventTable += event
          }
          logger.info(event.toString)
          RiderConfirm(riders(bibNumber), event)
        }
      }
      sender ! riderConfirm
    case RestStopCounts => sender ! riderCounts.toJson.toString
    case RiderUpdates => {
      val updates: List[RiderSummary] = db.withSession { implicit session =>
        val events = latestEventPerRider.sortBy(_.timestamp).list
        events.map({ x =>
          RiderSummary(x.bibNumber, riders(x.bibNumber).name, restStop(x).name, hhmmssFormatter.print(x.timestamp.toDateTime(DateTimeZone.forID("America/Los_Angeles"))))
        })
      }
      sender ! updates.toJson.toString
    }
  }
}

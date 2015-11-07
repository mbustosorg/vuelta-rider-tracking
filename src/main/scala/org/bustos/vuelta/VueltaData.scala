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
  var ridersByName: Map[String, Rider] = {
    val fullList = db.withSession { implicit session =>
      riderTable.list.map({ x => (x.name -> Rider(x.bibNumber, x.name, x.registrationDate))})
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
    case Rider(bibNumber, name, addTime) => {
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
      if (rider.bibNumber != 0) sender ! Rider(0, "", null)
      else {
        val newRider = Rider(bibNumber, name, addTime)
        db.withSession { implicit session =>
          riderTable += newRider
        }
        riders += (bibNumber -> newRider)
        ridersByName += (name -> newRider)
        sender ! newRider
      }
    }
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
    case RiderDelete(bibNumber) => {
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
      if (rider.bibNumber > 0) {
        db.withSession { implicit session =>
          riderTable.filter(_.bibNumber === bibNumber).delete
        }
        riders -= bibNumber
        ridersByName -= rider.name
      }
      sender ! rider
    }
    case RiderUpdateBib(bibNumber, name) => {
      val rider = {
        if (ridersByName.contains(name)) ridersByName(name)
        else {
          val riders = db.withSession { implicit session =>
            riderTable.filter(_.name === name).list
          }
          if (riders.isEmpty) Rider(0, "", null)
          else riders.head
        }
      }
      if (rider.bibNumber > 0) {
        val updatedRider = Rider(bibNumber, name, new org.joda.time.DateTime(org.joda.time.DateTimeZone.UTC))
        db.withSession { implicit session =>
          riderTable.filter(_.bibNumber === rider.bibNumber).delete
          riderTable += updatedRider
        }
        riders -= bibNumber
        ridersByName -= name
        riders += (bibNumber -> updatedRider)
        ridersByName += (name -> updatedRider)
        sender ! updatedRider
      } else sender ! rider
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
          val stop = {
            if (restStop(x).name == "Off Course") {
              val lat = x.latitude
              val lon = x.longitude
              f"$lat%1.4f" + ", " + f"$lon%1.4f"
            } else restStop(x).name
          }
          RiderSummary(x.bibNumber, riders(x.bibNumber).name, stop, hhmmssFormatter.print(x.timestamp.toDateTime(DateTimeZone.forID("America/Los_Angeles"))))
        })
      }
      sender ! updates.toJson.toString
    }
  }
}

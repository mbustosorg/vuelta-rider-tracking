package org.bustos.vuelta

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import org.bustos.vuelta.VueltaTables.{RiderEvent, Rider}
import org.slf4j.LoggerFactory
import scala.util.Properties.envOrElse
import scala.slick.driver.MySQLDriver.simple._
import org.joda.time._
import spray.json._

object VueltaData {
  case class RiderRequest(bibNumber: Int)
  case class RiderUpdate(bibNumber: Int, latitude: Double, longitude: Double)
  case class RiderConfirm(rider: Rider, update: RiderEvent)

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

object VueltaDataJsonProtocol extends DefaultJsonProtocol {

  import VueltaData._
  import VueltaJsonProtocol._
  implicit val riderUpdate = jsonFormat3(RiderUpdate)
  implicit val riderConfirm = jsonFormat2(RiderConfirm)

}

class VueltaData extends Actor with ActorLogging {

  import VueltaData._

  val logger =  LoggerFactory.getLogger(getClass)

  implicit val defaultTimeout = Timeout(10000)

  var riders: Map[Int, VueltaTables.Rider] = {
    val fullList = db.withSession { implicit session =>
      VueltaTables.riderTable.list.map({ x => (x.bibNumber -> Rider(x.bibNumber, x.name, x.registrationDate))})
    }
    fullList.toMap
  }
  var riderEvents = Map.empty[Int, VueltaTables.RiderEvent]

  def receive = {
    case RiderRequest(bibNumber) => {
      val rider = {
        if (riders.contains(bibNumber)) riders(bibNumber)
        else {
          val riders = db.withSession { implicit session =>
            VueltaTables.riderTable.filter(_.bibNumber === bibNumber).list
          }
          if (riders.isEmpty) Rider(0, "", null)
          else riders.head
        }
      }
      sender ! rider
    }
    case RiderUpdate(bibNumber, latitude, longitude) =>
      val riderConfirm = {
        if (!riders.contains(bibNumber)) RiderConfirm(Rider(0, "", null), RiderEvent(0, 0.0, 0.0, new DateTime))
        else {
          val event = RiderEvent(bibNumber, latitude, longitude, new DateTime)
          db.withSession { implicit session =>
            VueltaTables.riderEventTable += event
          }
          logger.info(event.toString)
          RiderConfirm(riders(bibNumber), event)
        }
      }
      sender ! riderConfirm
  }
}

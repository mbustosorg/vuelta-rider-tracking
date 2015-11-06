package org.bustos.vuelta

import org.joda.time._
import java.sql.Timestamp
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}

import scala.slick.driver.MySQLDriver.simple._
import spray.json._

object VueltaTables {
  case class RestStopCounts()
  case class RiderUpdates()
  case class RiderRequest(bibNumber: Int)
  case class RiderDelete(bibNumber: Int)
  case class RiderUpdate(bibNumber: Int, latitude: Double, longitude: Double)
  case class RestStop(name: String, latitude: Double, longitude: Double)
  case class RiderConfirm(rider: Rider, update: RiderEvent)
  case class Rider(bibNumber: Int, name: String, registrationDate: DateTime)
  case class RiderEvent(bibNumber: Int, latitude: Double, longitude: Double, timestamp: DateTime)
  case class RiderSummary(bibNumber: Int, name: String, stop: String, timestamp: String)

  val RestStops = List(
    RestStop("Start", 37.850787, -122.258015),
    RestStop("Moraga", 37.838825, -122.126016),
    RestStop("Briones", 37.925907, -122.162653),
    RestStop("Tilden", 37.904802, -122.244842),
    RestStop("End", 37.850787, -122.258015)
  )
  val OffCourse = RestStop("Off Course", 0.0, 0.0)

  val riderTable = TableQuery[RiderTable]
  val riderEventTable = TableQuery[RiderEventTable]
  val latestEventPerRider = TableQuery[LatestEventPerRider]

  implicit def dateTime =
    MappedColumnType.base[DateTime, Timestamp](
       dt => new Timestamp(dt.getMillis),
       ts => new DateTime(ts.getTime)
    )
}

object VueltaJsonProtocol extends DefaultJsonProtocol {

  import VueltaTables._

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    private val parserISO: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis();
    override def write(obj: DateTime) = JsString(parserISO.print(obj))
    override def read(json: JsValue) : DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case _ => throw new DeserializationException("Error info you want here ...")
    }
  }

  implicit val restStop = jsonFormat3(RestStop)
  implicit val riderUpdate = jsonFormat3(RiderUpdate)
  implicit val riderFormat = jsonFormat3(Rider)
  implicit val riderEventFormat = jsonFormat4(RiderEvent)
  implicit val riderConfirm = jsonFormat2(RiderConfirm)
  implicit val riderSummary = jsonFormat4(RiderSummary)

}

class RiderTable(tag: Tag) extends Table[VueltaTables.Rider](tag, "rider") {
  import VueltaTables.dateTime

  def bibNumber = column[Int]("bibNumber")
  def name = column[String]("name")
  def registrationDate = column[DateTime]("registrationDate")

  def * = (bibNumber, name, registrationDate) <> (VueltaTables.Rider.tupled, VueltaTables.Rider.unapply)
}

class RiderEventTable(tag: Tag) extends Table[VueltaTables.RiderEvent](tag, "riderEvent") {
  import VueltaTables.dateTime

  def bibNumber = column[Int]("bibNumber")
  def latitude = column[Double]("latitude")
  def longitude = column[Double]("longitude")
  def timestamp = column[DateTime]("timestamp")

  def * = (bibNumber, latitude, longitude, timestamp) <> (VueltaTables.RiderEvent.tupled, VueltaTables.RiderEvent.unapply)
}

class LatestEventPerRider(tag: Tag) extends Table[VueltaTables.RiderEvent](tag, "latestEventPerRider") {
  import VueltaTables.dateTime

  def bibNumber = column[Int]("bibNumber")
  def latitude = column[Double]("latitude")
  def longitude = column[Double]("longitude")
  def timestamp = column[DateTime]("timestamp")

  def * = (bibNumber, latitude, longitude, timestamp) <> (VueltaTables.RiderEvent.tupled, VueltaTables.RiderEvent.unapply)
}
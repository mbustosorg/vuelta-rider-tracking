package org.bustos.vuelta

import org.joda.time._
import java.sql.Timestamp
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}

import scala.slick.driver.MySQLDriver.simple._
import spray.json._

object VueltaTables {
  case class Rider(bibNumber: Int, name: String, registrationDate: DateTime)
  case class RiderEvent(bibNumber: Int, latitude: Double, longitude: Double, timestamp: DateTime)

  val riderTable = TableQuery[RiderTable]
  val riderEventTable = TableQuery[RiderEventTable]

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

  implicit val riderFormat = jsonFormat3(Rider)
  implicit val riderEventFormat = jsonFormat4(RiderEvent)

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
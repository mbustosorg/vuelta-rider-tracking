package org.bustos.vuelta

import javax.ws.rs.Path

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.annotations._
import com.wordnik.swagger.model.ApiInfo
import org.bustos.vuelta.VueltaData._
import org.bustos.vuelta.VueltaTables.{Rider}
import org.slf4j.LoggerFactory
import spray.http.MediaTypes._
import spray.json._
import spray.routing._

import scala.concurrent.duration._
import scala.reflect.runtime.universe._
import VueltaTables._
import VueltaJsonProtocol._
import spray.json._

class VueltaServiceActor extends HttpServiceActor with ActorLogging {

  override def actorRefFactory = context

  val vueltaRoutes = new VueltaRoutes {
    def actorRefFactory = context
  }

  def receive = runRoute(
    swaggerService.routes ~
    vueltaRoutes.routes ~
      get {getFromResourceDirectory("webapp")} ~
      get {getFromResource("webapp/index.html")})

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[VueltaRoutes])
    override def apiVersion = "1.0"
    override def baseUrl = "/"
    override def docsPath = "api-docs"
    override def actorRefFactory = context
    override def apiInfo = Some(new ApiInfo("Vuelta API",
      "API for interacting with the Vuelta Server.", "", "", "", ""))
  }
}

@Api(value = "/", description = "Primary Interface", produces = "application/json")
trait VueltaRoutes extends HttpService {

  val logger = LoggerFactory.getLogger(getClass)
  val system = ActorSystem("vueltaSystem")
  import system.dispatcher
  implicit val defaultTimeout = Timeout(10000 milliseconds)
  val vueltaData = system.actorOf(Props[VueltaData], "vueltaData")
  val routes = testRoute ~ nameForRider ~ updateRider ~ riderStatus ~ restStopCounts

  @Path("test")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Operates connectivity test")
  @ApiImplicitParams(Array())
  @ApiResponses(Array())
  def testRoute =
    path("test") {
      respondWithMediaType(`application/json`) { ctx =>
        ctx.complete("{\"response\": \"Server is OK\"}")
      }
    }

  @Path("riderName/{bibNumber}")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Name for rider")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "bibNumber", required = true, dataType = "string", paramType = "path", value = "Rider's bib number")
  ))
  @ApiResponses(Array())
  def nameForRider = get {
    pathPrefix("riderName" / IntNumber) { (bibNumber) =>
      pathEnd {
        respondWithMediaType(`application/json`) { ctx =>
          val future = vueltaData ? RiderRequest(bibNumber)
          future onSuccess {
            case Rider(number, name, datetime) => ctx.complete(Rider(number, name, datetime).toJson.toString)
          }
        }
      }
    }
  }

  @Path("updateRider/{bibNumber}")
  @ApiOperation(httpMethod = "POST", response = classOf[String], value = "Update position of rider")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "bibNumber", required = true, dataType = "string", paramType = "path", value = "Rider's bib number")
  ))
  def updateRider = post {
    pathPrefix("updateRider" / IntNumber) { (bibNumber) =>
      pathEnd {
        respondWithMediaType(`application/json`) { ctx =>
          val update = ctx.request.entity.data.asString.parseJson.convertTo[RiderUpdate]
          val future = vueltaData ? update
          future onSuccess {
            case VueltaTables.RiderConfirm(rider, update) => ctx.complete(VueltaTables.RiderConfirm(rider, update).toJson.toString)
          }
        }
      }
    }
  }

  @Path("riderStatus")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Latest rider updates")
  def riderStatus = get {
    path("riderStatus") {
      respondWithMediaType(`application/json`) { ctx =>
        val future = vueltaData ? RiderUpdates
        future onSuccess {
          case x: String => ctx.complete(x)
        }
      }
    }
  }

  @Path("restStopCounts")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Counts of riders by rest stop")
  def restStopCounts = get {
    path("restStopCounts") {
      respondWithMediaType(`application/json`) { ctx =>
        val future = vueltaData ? RestStopCounts
        future onSuccess {
          case x: String => ctx.complete(x)
        }
      }
    }
  }

}

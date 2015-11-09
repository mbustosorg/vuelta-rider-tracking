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
import spray.http.DateTime
import spray.http.{DateTime, HttpCookie}
import spray.http.MediaTypes._
import spray.json._
import spray.routing._

import scala.concurrent.duration._
import scala.reflect.runtime.universe._
import VueltaTables._
import VueltaJsonProtocol._
import spray.json._
import spray.http.StatusCodes._

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
trait VueltaRoutes extends HttpService with UserAuthentication {

  import UserAuthentication._
  import java.net.InetAddress

  val logger = LoggerFactory.getLogger(getClass)
  val system = ActorSystem("vueltaSystem")

  import system.dispatcher

  val vueltaData = system.actorOf(Props[VueltaData], "vueltaData")
  val routes = testRoute ~
    nameForRider ~
    deleteRider ~
    addRider ~
    updateRider ~
    observeRider ~
    riderStatus ~
    restStopCounts ~
    login ~
    reports ~
    admin

  val authenticationRejection = RejectionHandler {
    case AuthenticationRejection(message) :: _ => complete(400, message)
  }

  val authorizationRejection = RejectionHandler {
    case AuthenticationRejection(message) :: _ => getFromResource("webapp/login.html")
  }

  val secureCookies: Boolean = {
    // Don't require HTTPS if running in development
    val hostname = InetAddress.getLocalHost.getHostName
    hostname != "localhost" && !hostname.contains("pro")
  }

  def redirectToHttps: Directive0 = {
    requestUri.flatMap { uri =>
      redirect(uri.copy(scheme = "https"), MovedPermanently)
    }
  }

  val isHttpsRequest: RequestContext => Boolean = { ctx =>
    (ctx.request.uri.scheme == "https" || ctx.request.headers.exists(h => h.is("x-forwarded-proto") && h.value == "https")) && secureCookies
  }

  def enforceHttps: Directive0 = {
    extract(isHttpsRequest).flatMap(
      if (_) pass
      else redirectToHttps
    )
  }

  val keyLifespanMillis = 120000 * 1000 // 2000 minutes
  val expiration = DateTime.now + keyLifespanMillis
  val SessionKey = "VUELTA_SESSION"
  val UserKey = "VUELTA_USER"

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

  @Path("rider/{bibNumber}")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Name for rider")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "bibNumber", required = true, dataType = "string", paramType = "path", value = "Rider's bib number")
  ))
  @ApiResponses(Array())
  def nameForRider = get {
    pathPrefix("rider" / IntNumber) { (bibNumber) =>
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

  @Path("rider/{bibNumber}")
  @ApiOperation(httpMethod = "POST", response = classOf[String], value = "Add a new rider")
  def addRider =
    post {
      path("rider" / IntNumber) { (bibNumber) =>
        formFields('name) { (name) =>
          respondWithMediaType(`application/json`) { ctx =>
            val future = vueltaData ? Rider(bibNumber, name, new org.joda.time.DateTime(org.joda.time.DateTimeZone.UTC))
            future onSuccess {
              case Rider(number, name, datetime) => ctx.complete(Rider(number, name, datetime).toJson.toString)
            }
          }
        }
      }
    }

  @Path("rider/{bibNumber}/delete")
  @ApiOperation(httpMethod = "POST", response = classOf[String], value = "Delete rider")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "bibNumber", required = true, dataType = "string", paramType = "path", value = "Rider's bib number")
  ))
  @ApiResponses(Array())
  def deleteRider = post {
    pathPrefix("rider" / IntNumber / "delete") { (bibNumber) =>
      respondWithMediaType(`application/json`) { ctx =>
        val future = vueltaData ? RiderDelete(bibNumber)
        future onSuccess {
          case Rider(number, name, datetime) => ctx.complete(Rider(number, name, datetime).toJson.toString)
        }
      }
    }
  }

  @Path("rider/{bibNumber}/update")
  @ApiOperation(httpMethod = "POST", response = classOf[String], value = "Update a rider's bibNumber")
  def updateRider =
    post {
      path("rider" / IntNumber / "update") { (bibNumber) =>
        formFields('name) { (name) =>
          respondWithMediaType(`application/json`) { ctx =>
            val future = vueltaData ? RiderUpdateBib(bibNumber, name)
            future onSuccess {
              case Rider(number, name, datetime) => ctx.complete(Rider(number, name, datetime).toJson.toString)
            }
          }
        }
      }
    }

  @Path("rider/{bibNumber}/observe")
  @ApiOperation(httpMethod = "POST", response = classOf[String], value = "Update position of rider")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "bibNumber", required = true, dataType = "string", paramType = "path", value = "Rider's bib number")
  ))
  def observeRider = post {
    pathPrefix("rider" / IntNumber / "observe") { (bibNumber) =>
      pathEnd {
        respondWithMediaType(`application/json`) { ctx =>
          val update = ctx.request.entity.data.asString.parseJson.convertTo[RiderUpdate]
          val future = vueltaData ? update
          future onSuccess {
            case RiderConfirm(rider, update) => ctx.complete(RiderConfirm(rider, update).toJson.toString)
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

  @Path("report")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Get the rider reports")
  def reports =
    path("report") {
      getFromResource("webapp/report.html")
    }

  @Path("admin")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Admin into the system")
  def admin = get {
    path("admin.html") {
      cookie("VUELTA_SESSION") { sessionId => {
        cookie("VUELTA_USER") { username => {
          handleRejections(authorizationRejection) {
            authenticate(authenticateSessionId(sessionId.content, username.content)) { authentication =>
              getFromResource("webapp/admin.html")
            }
          }
        }
        }
      }
      } ~ getFromResource("webapp/login.html")
    }
  }

  @Path("login")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Log into the system")
  def login =
    post {
      path("login") {
        formFields('inputEmail, 'inputPassword) { (inputEmail, inputPassword) =>
          handleRejections(authenticationRejection) {
            authenticate(authenticateUser(inputEmail, inputPassword)) { authentication =>
              setCookie(HttpCookie(SessionKey, content = authentication.token, expires = Some(expiration))) {
                setCookie(HttpCookie(UserKey, content = inputEmail, expires = Some(expiration))) {
                  complete("/admin.html")
                }
              }
            }
          }
        }
      }
    }

}

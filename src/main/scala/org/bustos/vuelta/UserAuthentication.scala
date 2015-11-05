package org.bustos.vuelta

import scala.concurrent._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.{ ActorRef, ActorSystem }
import spray.routing._
import spray.routing.authentication._
import org.slf4j.{Logger, LoggerFactory}

object UserAuthentication {
  case class AuthenticationRejection(reason: String) extends Rejection
  case class AuthenticationRequest(username: String, password: String)
  case class SessionAuthenticationRequest(sessionId: String, username: String)
  case class Authenticated(token: String)
  case class AuthenticationRejected(reason: String)
  case class UserRegistration(email: String, firstName: String, lastName: String, password: String)
  case class LogoutRequest(sessionId: String, username: String)}

trait UserAuthentication {

  import UserAuthentication._

  implicit val defaultTimeout = Timeout(30 seconds)

  val logger: Logger

  val authentications = Map(("m@bustos.org" -> "test"))
  var sessionIds = Map.empty[String, String]

  def authenticateUser(email: String, password: String)(implicit ec: ExecutionContext): ContextAuthenticator[Authenticated] = {
    ctx =>
    {
      logger.info("Authenticating User:" + email + ", *******")
      doUserAuth(email, password)
    }
  }

  def authenticateSessionId(sessionId: String, username: String)(implicit ec: ExecutionContext): ContextAuthenticator[Authenticated] = {
    ctx =>
    {
      logger.info("Authenticating Session: " + sessionId + ":" + username)
      doSessionAuth(sessionId, username)
    }
  }

  private def doUserAuth(email: String, password: String)(implicit ec: ExecutionContext): Future[Authentication[Authenticated]] = {
    Future {
      val result = {
        if (authentications.contains(email)) {
          if (authentications(email) == password) {
            val sessionId = java.util.UUID.randomUUID.toString
            sessionIds += (email -> sessionId)
            Authenticated(sessionId)
          }
          else AuthenticationRejection("Invalid Password")
        } else AuthenticationRejection("Unknown Email")
      }
      Either.cond(result match {
        case Authenticated(token) => true
        case _ => false
      },
        result match {case x: Authenticated => x},
        result match {case AuthenticationRejection(message) => AuthenticationRejection(message)}
      )
    }
  }

  private def doSessionAuth(sessionId: String, email: String)(implicit ec: ExecutionContext): Future[Authentication[Authenticated]] = {
    Future {
      val result = {
        if (sessionIds.contains(email)) {
          if (sessionIds(email) == sessionId) {
            Authenticated(sessionId)
          }
          else AuthenticationRejection("Invalid Login")
        } else AuthenticationRejection("Unknown Email")
      }
      Either.cond(result match {
        case Authenticated(token) => true
        case _ => false
      },
        result match {case x: Authenticated => x},
        result match {case AuthenticationRejection(message) => AuthenticationRejection(message)}
      )
    }
  }
}

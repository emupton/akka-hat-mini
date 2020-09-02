package com.hatmini

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.sslconfig.util.ConfigLoader
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.emarsys.jwt.akka.http.JwtAuthentication
import com.emarsys.jwt.akka.http.JwtConfig
import com.hatmini.util.CirceSupport
import com.hatmini.auth.AuthService
import pdi.jwt.JwtClaim
import com.hatmini.PDA.PDAService
import io.circe.generic.auto._
import org.mongodb.scala.bson.BsonDocument
import com.hatmini.util.Model._
//#main-class
object ServiceBootstrap extends LazyLogging with CirceSupport with App {
  val config = ConfigFactory.load()

  implicit lazy val system: ActorSystem = ActorSystem("hat-akka-server")
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  lazy val authService = new AuthService(config.getConfig("service.auth0"))

  lazy val pdaService = new PDAService()

  implicit lazy val timeout: Timeout = 10.seconds
  val testRoutes = {
    authService.auth0JwtValidationDirective { jwt: JwtClaim =>
      path("status") {
        get {
          complete("OK")
        }
      } ~
        pathPrefix("resource") { //access resources as an owner
          pathPrefix(Segment) { (resourceType) =>
            {
              get {
                complete(
                  pdaService.getResources(
                    resourceType,
                    jwt.subject.get
                  )
                )

              } ~ post {
                entity(as[BsonDocument]) { document =>
                  {
                    complete(
                      pdaService.createResource(
                        resourceType,
                        document,
                        jwt.subject.get
                      )
                    )
                  }
                }
              }
            }
          }
        } ~ pathPrefix("data-debit") { //share data with another user
        pathPrefix(Segment) { (resourceType) =>
          {
            post {
              entity(as[DataDebitRequest]) {
                dataDebitRequest: DataDebitRequest =>
                  complete(
                    pdaService.dataDebit(
                      resourceType,
                      jwt.subject.get,
                      dataDebitRequest.dataRecipient,
                      dataDebitRequest.permissions
                    )
                  )
              }
            }
          }
        }
      } ~ pathPrefix("client") { //access resources as a 3rd party
        pathPrefix(Segment) { (dataOwner) =>
          {
            pathPrefix(Segment) { resourceType =>
              get {
                complete(
                  pdaService.getResourceAsNonOwner(
                    resourceType,
                    dataOwner,
                    jwt.subject.get
                  )
                )
              } ~ post {
                entity(as[BsonDocument]) { document =>
                  complete(
                    pdaService.createResourceAsNonOwner(
                      resourceType,
                      dataOwner,
                      jwt.subject.get,
                      document
                    )
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  val concatRoutes =
    concat(cors() { testRoutes }) //TODO: increase specificity of cors

  val interface = "0.0.0.0"
  val port = 6000

  Http().bindAndHandle(concatRoutes, interface, port)

  logger.info(s"Server online, $interface:$port")

}

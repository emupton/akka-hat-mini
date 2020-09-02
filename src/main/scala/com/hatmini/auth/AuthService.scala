package com.hatmini.auth

import com.typesafe.config.Config
import com.auth0.jwk.{UrlJwkProvider}
import pdi.jwt.{JwtAlgorithm, JwtBase64, JwtClaim}
import scala.util.Try
import pdi.jwt.JwtCirce
import scala.util.Failure
import scala.util.Success
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.headers.HttpChallenge
import java.time.Clock

class AuthService(config: Config) {

  private val jwtRegex = """(.+?)\.(.+?)\.(.+?)""".r
  implicit val clock: Clock = Clock.systemDefaultZone()

  private def domain = config.getString("domain")
  private def audience = config.getString("audience")
  private def issuer = s"https://$domain/"

  def auth0JwtValidationDirective: Directive1[pdi.jwt.JwtClaim] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(token) =>
        validateJwt(stripBearerPrefix(token)) match {
          case Success(value) => provide[pdi.jwt.JwtClaim](value)
          case Failure(reason) => {
            val reason2 = reason
            reject(
              AuthenticationFailedRejection(
                AuthenticationFailedRejection.CredentialsRejected,
                HttpChallenge("Basic", "JWT")
              )
            )
          }
        }
      case _ =>
        reject(
          AuthenticationFailedRejection(
            AuthenticationFailedRejection.CredentialsMissing,
            HttpChallenge("Basic", "JWT")
          )
        )
    }
  }

  private def stripBearerPrefix(token: String): String = {
    token.stripPrefix("Bearer ")
  }

  def validateJwt(token: String): Try[JwtClaim] =
    for {
      jwk <- getJwk(token) // Get the secret key for this token
      claims <- JwtCirce.decode(
        token,
        jwk.getPublicKey,
        Seq(JwtAlgorithm.RS256)
      ) // Decode the token using the secret key
      result <-
        validateClaims(claims) // validate the data stored inside the token
    } yield claims

  private val splitToken = (jwt: String) =>
    jwt match {
      case jwtRegex(header, body, sig) => Success((header, body, sig))
      case _ =>
        Failure(new Exception("Token does not match the correct pattern"))
    }

  private val decodeElements = (data: Try[(String, String, String)]) =>
    data map {
      case (header, body, sig) =>
        (JwtBase64.decodeString(header), JwtBase64.decodeString(body), sig)
    }

  private val getJwk = (token: String) =>
    (splitToken andThen decodeElements)(token) flatMap {
      case (header, _, _) =>
        val jwtHeader = JwtCirce.parseHeader(header) // extract the header
        val jwkProvider = new UrlJwkProvider(s"https://$domain")

        // Use jwkProvider to load the JWKS data and return the JWK
        jwtHeader.keyId.map { k =>
          Try(jwkProvider.get(k))
        } getOrElse Failure(new Exception("Unable to retrieve kid"))
    }

  private val validateClaims = (claims: JwtClaim) => {
    if (claims.isValid(issuer, audience)) {
      Success(claims)
    } else {
      val claims2 = claims
      Failure(new Exception("The JWT did not pass validation"))
    }
  }

}

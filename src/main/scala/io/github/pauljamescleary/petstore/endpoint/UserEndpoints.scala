package io.github.pauljamescleary.petstore.endpoint

import cats.effect.Effect
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.model.User
import io.github.pauljamescleary.petstore.service.UserService
import org.http4s.circe._
import org.http4s.{EntityDecoder, HttpService}
import org.http4s.dsl.Http4sDsl

class UserEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  /* Jsonization of our User type */
  implicit val userDecoder: EntityDecoder[F, User] = jsonOf[F, User]

  private def signupEndpoint(userService: UserService[F]): HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "users" =>
        for {
          user <- req.as[User]
          result <- userService.createUser(user)
          response <- Ok(result.asJson)
        } yield response
    }

  def endpoints(userService: UserService[F]): HttpService[F] = signupEndpoint(userService)
}

object UserEndpoints {
  def endpoints[F[_]: Effect](userService: UserService[F]): HttpService[F] =
    new UserEndpoints[F].endpoints(userService)
}

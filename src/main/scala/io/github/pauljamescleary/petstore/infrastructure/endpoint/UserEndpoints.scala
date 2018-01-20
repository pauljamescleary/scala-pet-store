package io.github.pauljamescleary.petstore.infrastructure.endpoint

import cats.effect.Effect
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService}

import scala.language.higherKinds
import io.github.pauljamescleary.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}
import io.github.pauljamescleary.petstore.domain.users.{User, UserService}

class UserEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  import Pagination._
  /* Jsonization of our User type */
  implicit val userDecoder: EntityDecoder[F, User] = jsonOf[F, User]

  private def signupEndpoint(userService: UserService[F]): HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "users" =>
        val action = for {
          user <- req.as[User]
          result <- userService.createUser(user).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserAlreadyExistsError(existing)) =>
            Conflict(s"The user with user name ${existing.userName} already exists")
        }
    }

  private def updateEndpoint(userService: UserService[F]): HttpService[F] =
    HttpService[F] {
      case req @ PUT -> Root / "users" =>
        val action = for {
          user <- req.as[User]
          result <- userService.update(user).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserNotFoundError) => NotFound("User not found")
        }
    }

  private def listEndpoint(userService: UserService[F]): HttpService[F] =
    HttpService[F] {
      case GET -> Root / "users" :? PageSizeMatcher(pageSize) :? OffsetMatcher(offset) =>
        for {
          retrived <- userService.list(pageSize, offset)
          resp <- Ok(retrived.asJson)
        } yield resp
    }

  private def searchByName(userService: UserService[F]): HttpService[F] =
    HttpService[F] {
      case GET -> Root / "users" / userName =>
        userService.getUserByName(userName).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(UserNotFoundError) => NotFound("The user was not found")
        }
    }

  private def deleteUserEndpoint(userService: UserService[F]): HttpService[F] =
    HttpService[F] {
      case DELETE -> Root / "users" / userByName =>
        for {
          _ <- userService.deleteByName(userByName)
          resp <- Ok()
        } yield resp
    }


  def endpoints(userService: UserService[F]): HttpService[F] =
    signupEndpoint(userService) <+>
    updateEndpoint(userService) <+>
    listEndpoint(userService)   <+>
    searchByName(userService)   <+>
    deleteUserEndpoint(userService)
}

object UserEndpoints {
  def endpoints[F[_]: Effect](userService: UserService[F]): HttpService[F] =
    new UserEndpoints[F].endpoints(userService)
}

package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.domain.authentication.CryptService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService, Response}

import scala.language.higherKinds
import domain._
import domain.users._
import domain.authentication._
import tsec.authentication._
import tsec.passwordhashers.PasswordHash

class UserEndpoints[F[_]: Effect, A, K] extends Http4sDsl[F] {
  type AuthService = AuthenticatorService[F, Long, User, K]
  type CS = CryptService[F, A]

  import Pagination._
  /* Jsonization of our User type */
  implicit val userDecoder: EntityDecoder[F, User] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf

  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  private def loginEndpoint(userService: UserService[F], crypt: CS, Auth: AuthService): HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "login" =>
        val action: EitherT[F, UserAuthenticationFailedError, Response[F]] = for {
          login <- EitherT.liftF(req.as[LoginRequest])
          name = login.userName
          user <- userService.getUserByName(name).leftMap(_ => UserAuthenticationFailedError(name))
          valid <- EitherT.liftF(crypt.check(login.password, PasswordHash[A](user.hash)))
          resp <- EitherT.liftF(Ok())
        } yield resp

        // Handle error cases:
        action.value.flatMap{
          case Right(resp) => resp.pure[F]
          case Left(UserAuthenticationFailedError(name)) => Conflict(s"Authentication failed for user $name")
        }
    }

  private def signupEndpoint(userService: UserService[F], crypt: CS, Auth: AuthService): HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "users" =>
        val action = for {
          signup <- req.as[SignupRequest]
          hash <- crypt.hash(signup.password)
          user <- signup.asUser(hash).pure[F]
          result <- userService.createUser(user).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserAlreadyExistsError(existing)) =>
            Conflict(s"The user with user name ${existing.userName} already exists")
        }
    }

  private def updateEndpoint(userService: UserService[F], Auth: AuthService): HttpService[F] =
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

  private def listEndpoint(userService: UserService[F], Auth: AuthService): HttpService[F] =
    HttpService[F] {
      case GET -> Root / "users" :? PageSizeMatcher(pageSize) :? OffsetMatcher(offset) =>
        for {
          retrived <- userService.list(pageSize, offset)
          resp <- Ok(retrived.asJson)
        } yield resp
    }

  private def searchByNameEndpoint(userService: UserService[F], Auth: AuthService): HttpService[F] =
    HttpService[F] {
      case GET -> Root / "users" / userName =>
        userService.getUserByName(userName).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(UserNotFoundError) => NotFound("The user was not found")
        }
    }

  private def deleteUserEndpoint(userService: UserService[F], Auth: AuthService): HttpService[F] =
    HttpService[F] {
      case DELETE -> Root / "users" / userName =>
        for {
          _ <- userService.deleteByUserName(userName)
          resp <- Ok()
        } yield resp
    }


  def endpoints(userService: UserService[F], crypt: CS, Auth: AuthService): HttpService[F] =
    loginEndpoint(userService, crypt, Auth) <+>
    signupEndpoint(userService, crypt, Auth) <+>
    updateEndpoint(userService, Auth) <+>
    listEndpoint(userService, Auth)   <+>
    searchByNameEndpoint(userService, Auth)   <+>
    deleteUserEndpoint(userService, Auth)
}

object UserEndpoints {
  def endpoints[F[_]: Effect, A, K](
    userService: UserService[F],
    cryptService: CryptService[F, A],
    Auth: AuthenticatorService[F, Long, User, K]
  ): HttpService[F] =
    new UserEndpoints[F, A, K].endpoints(userService, cryptService, Auth)
}

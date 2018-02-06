package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService, Response}
import tsec.authentication._
import tsec.authentication.AuthenticatorService
import tsec.passwordhashers.PasswordHash

import domain._
import domain.users._
import infrastructure.authentication._


object AuthService{
  type AuthService[F[_], K] = AuthenticatorService[F, Long, User, K]
}

import AuthService._

class UserEndpoints[F[_]: Effect, A, K] extends Http4sDsl[F] {
  type CS = CryptService[F, A]
  type AS = AuthService[F, K]

  import Pagination._
  /* Jsonization of our User type */
  implicit val userDecoder: EntityDecoder[F, User] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf

  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf

//  implicit val webTokenEncoder: EntityEncoder[F, K] = jsonEncoderOf[F, K]

  private def loginEndpoint(userService: UserService[F], authService: AS, crypt: CS): HttpService[F] =
    HttpService {
      case req@POST -> Root / "login" =>
        val action: EitherT[F, UserAuthenticationFailedError, Response[F]] = for {
          login <- EitherT.liftF(req.as[LoginRequest])
          name = login.userName
          user <- userService.getUserByName(name).leftMap(_ => UserAuthenticationFailedError(name))
          valid <- EitherT.liftF(crypt.check(login.password, PasswordHash[A](user.hash)))
          tok <- EitherT.fromOptionF(user.id.traverse(authService.create(_)), UserAuthenticationFailedError(name))
          resp <- EitherT.liftF(Ok())
        } yield authService.embed(resp, tok)

        // Handle error cases:
        action.value.flatMap {
          case Right(resp) => resp.pure[F]
          case Left(UserAuthenticationFailedError(name)) => BadRequest(s"Authentication failed for $name")
        }
    }

  private def signupEndpoint(userService: UserService[F], authService: AS, crypt: CS): HttpService[F] =
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

  private def updateEndpoint(userService: UserService[F], authService: AS): HttpService[F] = {
    HttpService {
      case req @ PUT -> Root / "users" =>
        val action = for {
          updateUser <- req.as[User]
          result <- userService.update(updateUser).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserNotFoundError) => NotFound("User not found")
        }
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

  private def searchByNameEndpoint(userService: UserService[F]): HttpService[F] =
    HttpService[F] {
      case GET -> Root / "users" / userName =>
        userService.getUserByName(userName).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(UserNotFoundError) => NotFound("The user was not found")
        }
    }

  private def deleteUserEndpoint(userService: UserService[F]): HttpService[F] =
    HttpService[F] {
      case DELETE -> Root / "users" / userName =>
        for {
          _ <- userService.deleteByUserName(userName)
          resp <- Ok()
        } yield resp
    }

  
  def endpoints(userService: UserService[F], authService: AS, crypt: CS): HttpService[F] =
    loginEndpoint(userService, authService, crypt) <+>
    signupEndpoint(userService, authService, crypt) <+>
    updateEndpoint(userService, authService) <+>
    listEndpoint(userService)   <+>
    searchByNameEndpoint(userService)   <+>
    deleteUserEndpoint(userService)
}

object UserEndpoints {
  def endpoints[F[_]: Effect, A, K](
    userService: UserService[F],
    cryptService: CryptService[F, A],
    authService: AuthService[F, K]
  ): HttpService[F] =
    new UserEndpoints[F, A, K].endpoints(userService, authService, cryptService)
}

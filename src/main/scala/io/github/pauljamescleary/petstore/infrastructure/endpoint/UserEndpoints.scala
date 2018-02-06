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
import tsec.passwordhashers.PasswordHash

import domain._
import domain.users._
import infrastructure.authentication._

import AuthService._

class UserEndpoints[F[_]: Effect, A, K](authService: AuthService[F, K]) extends Http4sDsl[F] {
  type CS = CryptService[F, A]
  import Pagination._

  /* Jsonization of our User type and requests */
  implicit val userDecoder: EntityDecoder[F, User] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf
  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  val Auth = SecuredRequestHandler(authService)

  private def loginEndpoint(userService: UserService[F], crypt: CS): HttpService[F] =
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

  private def signupEndpoint(userService: UserService[F], crypt: CS): HttpService[F] =
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

  private def updateEndpoint(userService: UserService[F]): HttpService[F] = {
    Auth {
      case req @ PUT -> Root / "users" asAuthed _ =>
        val action = for {
          updateUser <- req.request.as[User]
          result <- userService.update(updateUser).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserNotFoundError) => NotFound("User not found")
        }
    }
  }

  private def listEndpoint(userService: UserService[F]): HttpService[F] =
    Auth {
      case GET -> Root / "users" :? PageSizeMatcher(pageSize) :? OffsetMatcher(offset) asAuthed _ =>
        for {
          retrived <- userService.list(pageSize, offset)
          resp <- Ok(retrived.asJson)
        } yield resp
    }
  
  private def searchByNameEndpoint(userService: UserService[F]): HttpService[F] =
    Auth {
      case GET -> Root / "users" / userName asAuthed _ =>
        userService.getUserByName(userName).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(UserNotFoundError) => NotFound("The user was not found")
        }
    }

  private def deleteUserEndpoint(userService: UserService[F]): HttpService[F] =
    Auth {
      case DELETE -> Root / "users" / userName asAuthed _ =>
        for {
          _ <- userService.deleteByUserName(userName)
          resp <- Ok()
        } yield resp
    }

  
  def endpoints(userService: UserService[F], crypt: CS): HttpService[F] =
    loginEndpoint(userService, crypt) <+>
    signupEndpoint(userService, crypt) <+>
    updateEndpoint(userService) <+>
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
    new UserEndpoints[F, A, K](authService).endpoints(userService, cryptService)
}

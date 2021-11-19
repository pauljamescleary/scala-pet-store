package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.data.EitherT
import cats.effect.{IO, Sync}
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import domain._
import domain.users._
import domain.authentication._
import tsec.common.Verified
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.passwordhashers.{PasswordHash, PasswordHasher}
import tsec.authentication._

class UserEndpoints[A, Auth: JWTMacAlgo] extends Http4sDsl[IO] {
  import Pagination._

  /* Jsonization of our User type */

  implicit val userDecoder: EntityDecoder[IO, User] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[IO, LoginRequest] = jsonOf

  implicit val signupReqDecoder: EntityDecoder[IO, SignupRequest] = jsonOf

  private def loginEndpoint(
      userService: UserService,
      cryptService: PasswordHasher[IO, A],
      auth: Authenticator[IO, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[IO] =
    HttpRoutes.of[IO] { case req @ POST -> Root / "login" =>
      val action = for {
        login <- EitherT.liftF(req.as[LoginRequest])
        name = login.userName
        user <- EitherT(userService.getUserByName(name)).leftMap(_ => UserAuthenticationFailedError(name))
        checkResult <- EitherT.liftF(
          cryptService.checkpw(login.password, PasswordHash[A](user.hash)),
        )
        _ <-
          if (checkResult == Verified) EitherT.rightT[IO, UserAuthenticationFailedError](())
          else EitherT.leftT[IO, User](UserAuthenticationFailedError(name))
        token <- user.id match {
          case None => EitherT.liftF[IO, UserAuthenticationFailedError, User](IO.raiseError(new Exception("Impossible"))) // User is not properly modeled
          case Some(id) => EitherT.right[UserAuthenticationFailedError](auth.create(id))
        }
      } yield (user, token)

      action.value.flatMap {
        case Right((user, token)) => Ok(user.asJson).map(auth.embed(_, token))
        case Left(UserAuthenticationFailedError(name)) =>
          Forbidden(s"Authentication failed for user $name")
      }
    }

  private def signupEndpoint(
      userService: UserService,
      crypt: PasswordHasher[IO, A],
  ): HttpRoutes[IO] =
    HttpRoutes.of[IO] { case req @ POST -> Root =>
      val action = for {
        signup <- req.as[SignupRequest]
        hash <- crypt.hashpw(signup.password)
        user <- signup.asUser(hash).pure[IO]
        result <- userService.createUser(user)
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserAlreadyExistsError(existing)) =>
          Conflict(s"The user with user name ${existing.userName} already exists")
      }
    }

  private def updateEndpoint(userService: UserService): AuthEndpoint[IO, Auth] = {
    case req @ PUT -> Root / name asAuthed _ =>
      val action = for {
        user <- req.request.as[User]
        updated = user.copy(userName = name)
        result <- userService.update(updated)
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserNotFoundError) => NotFound("User not found")
      }
  }

  private def listEndpoint(userService: UserService): AuthEndpoint[IO, Auth] = {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
          offset,
        ) asAuthed _ =>
      for {
        retrieved <- userService.list(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  private def searchByNameEndpoint(userService: UserService): AuthEndpoint[IO, Auth] = {
    case GET -> Root / userName asAuthed _ =>
      userService.getUserByName(userName).flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(UserNotFoundError) => NotFound("The user was not found")
      }
  }

  private def deleteUserEndpoint(userService: UserService): AuthEndpoint[IO, Auth] = {
    case DELETE -> Root / userName asAuthed _ =>
      for {
        _ <- userService.deleteByUserName(userName)
        resp <- Ok()
      } yield resp
  }

  def endpoints(
      userService: UserService,
      cryptService: PasswordHasher[IO, A],
      auth: SecuredRequestHandler[IO, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[IO] = {
    val authEndpoints: AuthService[IO, Auth] =
      Auth.adminOnly {
        updateEndpoint(userService)
          .orElse(listEndpoint(userService))
          .orElse(searchByNameEndpoint(userService))
          .orElse(deleteUserEndpoint(userService))
      }

    val unauthEndpoints =
      loginEndpoint(userService, cryptService, auth.authenticator) <+>
        signupEndpoint(userService, cryptService)

    unauthEndpoints <+> auth.liftService(authEndpoints)
  }
}

object UserEndpoints {
  def endpoints[A, Auth: JWTMacAlgo](
      userService: UserService,
      cryptService: PasswordHasher[IO, A],
      auth: SecuredRequestHandler[IO, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[IO] =
    new UserEndpoints[A, Auth].endpoints(userService, cryptService, auth)
}

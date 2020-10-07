package io.github.pauljamescleary.petstore
package infrastructure
package endpoint

import cats.effect._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import tsec.passwordhashers.jca.BCrypt
import domain.users._
import domain.authentication._
import infrastructure.repository.inmemory.UserRepositoryInMemoryInterpreter
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.server.Router
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration._
import tsec.authentication.{JWTAuthenticator, SecuredRequestHandler}
import tsec.mac.jca.HMACSHA256
import org.scalatest.matchers.should.Matchers

class UserEndpointsSpec
    extends AnyFunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO]
    with LoginTest {
  def userRoutes(): HttpApp[IO] = {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val key = HMACSHA256.unsafeGenerateKey
    val jwtAuth = JWTAuthenticator.unbacked.inBearerToken(1.day, None, userRepo, key)
    val usersEndpoint = UserEndpoints.endpoints(
      userService,
      BCrypt.syncPasswordHasher[IO],
      SecuredRequestHandler(jwtAuth),
    )
    Router(("/users", usersEndpoint)).orNotFound
  }

  test("create user and log in") {
    val userEndpoint = userRoutes()

    forAll { userSignup: SignupRequest =>
      val (_, authorization) = signUpAndLogIn(userSignup, userEndpoint).unsafeRunSync()
      authorization shouldBe defined
    }
  }

  test("update user") {
    val userEndpoint = userRoutes()

    forAll { userSignup: SignupRequest =>
      (for {
        loginResp <- signUpAndLogInAsAdmin(userSignup, userEndpoint)
        (createdUser, authorization) = loginResp
        userToUpdate = createdUser.copy(lastName = createdUser.lastName.reverse)
        updateUser <- PUT(userToUpdate, Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        updateUserAuth = updateUser.putHeaders(authorization.get)
        updateResponse <- userEndpoint.run(updateUserAuth)
        updatedUser <- updateResponse.as[User]
      } yield {
        updateResponse.status shouldEqual Ok
        updatedUser.lastName shouldEqual createdUser.lastName.reverse
        createdUser.id shouldEqual updatedUser.id
      }).unsafeRunSync()
    }
  }

  test("get user by userName") {
    val userEndpoint = userRoutes()

    forAll { userSignup: SignupRequest =>
      (for {
        loginResp <- signUpAndLogInAsAdmin(userSignup, userEndpoint)
        (createdUser, authorization) = loginResp
        getRequest <- GET(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        getRequestAuth = getRequest.putHeaders(authorization.get)
        getResponse <- userEndpoint.run(getRequestAuth)
        getUser <- getResponse.as[User]
      } yield {
        getResponse.status shouldEqual Ok
        createdUser.userName shouldEqual getUser.userName
      }).unsafeRunSync()
    }
  }

  test("delete user by userName") {
    val userEndpoint = userRoutes()

    forAll { userSignup: SignupRequest =>
      (for {
        loginResp <- signUpAndLogInAsCustomer(userSignup, userEndpoint)
        (createdUser, Some(authorization)) = loginResp
        deleteRequest <- DELETE(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        deleteRequestAuth = deleteRequest.putHeaders(authorization)
        deleteResponse <- userEndpoint.run(deleteRequestAuth)
      } yield deleteResponse.status shouldEqual Unauthorized).unsafeRunSync()
    }

    forAll { userSignup: SignupRequest =>
      (for {
        loginResp <- signUpAndLogInAsAdmin(userSignup, userEndpoint)
        (createdUser, Some(authorization)) = loginResp
        deleteRequest <- DELETE(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        deleteRequestAuth = deleteRequest.putHeaders(authorization)
        deleteResponse <- userEndpoint.run(deleteRequestAuth)
        getRequest <- GET(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        getRequestAuth = getRequest.putHeaders(authorization)
        getResponse <- userEndpoint.run(getRequestAuth)
      } yield {
        deleteResponse.status shouldEqual Ok
        // The user not the token longer exist
        getResponse.status shouldEqual Unauthorized
      }).unsafeRunSync()
    }
  }
}

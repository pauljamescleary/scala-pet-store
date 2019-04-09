package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import org.scalatest._
import cats.effect._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.circe._

import tsec.passwordhashers.jca.BCrypt

import domain.users._
import domain.authentication._
import infrastructure.repository.inmemory.UserRepositoryInMemoryInterpreter

import org.http4s.client.dsl.Http4sClientDsl
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class UserEndpointsSpec
  extends FunSuite
  with Matchers
  with ScalaCheckPropertyChecks
  with PetStoreArbitraries
  with Http4sDsl[IO]
  with Http4sClientDsl[IO] {

  implicit val userEnc : EntityEncoder[IO, User] = jsonEncoderOf
  implicit val userDec : EntityDecoder[IO, User] = jsonOf
  implicit val signupRequestEnc : EntityEncoder[IO, SignupRequest] = jsonEncoderOf
  implicit val signupRequestDec : EntityDecoder[IO, SignupRequest] = jsonOf

  test("create user") {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO]).orNotFound

    forAll { userSignup: SignupRequest =>
      (for {
        request <- POST(userSignup, Uri.uri("/users"))
        response <- userHttpService.run(request)
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }
  }

  test("update user") {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO]).orNotFound

    forAll { userSignup: SignupRequest =>
      (for {
        createRequest <- POST(userSignup, Uri.uri("/users"))
        createResponse <- userHttpService.run(createRequest)
        createdUser <- createResponse.as[User]
        userToUpdate = createdUser.copy(lastName = createdUser.lastName.reverse)
        updateUser <- PUT(userToUpdate, Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        updateResponse <- userHttpService.run(updateUser)
        updatedUser <- updateResponse.as[User]
      } yield {
        updateResponse.status shouldEqual Ok
        updatedUser.lastName shouldEqual createdUser.lastName.reverse
        createdUser.id shouldEqual updatedUser.id
      }).unsafeRunSync
    }
  }

  test("get user by userName") {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO]).orNotFound

    forAll { userSignup: SignupRequest =>
      (for {
        createRequest <- POST(userSignup, Uri.uri("/users"))
        createResponse <- userHttpService.run(createRequest)
        createdUser <- createResponse.as[User]
        getRequest <- GET(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        getResponse <- userHttpService.run(getRequest)
        getUser <- getResponse.as[User]
      } yield {
        getResponse.status shouldEqual Ok
        createdUser.userName shouldEqual getUser.userName
      }).unsafeRunSync
    }
  }


  test("delete user by userName") {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO]).orNotFound

    forAll { userSignup: SignupRequest =>
      (for {
        createRequest <- POST(userSignup, Uri.uri("/users"))
        createResponse <- userHttpService.run(createRequest)
        createdUser <- createResponse.as[User]
        deleteRequest <- DELETE(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        deleteResponse <- userHttpService.run(deleteRequest)
        getRequest <- GET(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        getResponse <- userHttpService.run(getRequest)
      } yield {
        createResponse.status shouldEqual Ok
        deleteResponse.status shouldEqual Ok
        getResponse.status shouldEqual NotFound
      }).unsafeRunSync
    }
  }
}

package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import org.scalatest._
import org.scalatest.prop.PropertyChecks
import cats.effect._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._

import tsec.passwordhashers.jca.BCrypt

import domain.users._
import domain.authentication._
import infrastructure.repository.inmemory.UserRepositoryInMemoryInterpreter


class UserEndpointsSpec
  extends FunSuite
  with Matchers
  with PropertyChecks
  with PetStoreArbitraries
  with Http4sDsl[IO] {

  test("create user") {

    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO])

    forAll { userSignup: SignupRequest =>
      (for {
        request <- Request[IO](Method.POST, Uri.uri("/users"))
          .withBody(userSignup.asJson)
        response <- userHttpService
          .run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }
  }

  test("update user") {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService: HttpService[IO] = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO])

    implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

    forAll { userSignup: SignupRequest =>
      (for {
        createRequest <- Request[IO](Method.POST, Uri.uri("/users"))
          .withBody(userSignup.asJson)
        createResponse <- userHttpService
          .run(createRequest)
          .getOrElse(fail(s"Request was not handled: $createRequest"))
        createdUser <- createResponse.as[User]
        userToUpdate = createdUser.copy(lastName = createdUser.lastName.reverse)
        updateRequest <- Request[IO](Method.PUT, Uri.unsafeFromString(s"/users/${createdUser.userName}"))
          .withBody(userToUpdate.asJson)
        updateResponse <- userHttpService
          .run(updateRequest)
          .getOrElse(fail(s"Request was not handled: $updateRequest"))
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
    val userHttpService: HttpService[IO] = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO])

    implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

    forAll { userSignup: SignupRequest =>
      (for {
        createRequest <- Request[IO](Method.POST, Uri.uri("/users"))
          .withBody(userSignup.asJson)
        createResponse <- userHttpService
          .run(createRequest)
          .getOrElse(fail(s"Request was not handled: $createRequest"))
        createdUser <- createResponse.as[User]
        getRequest = Request[IO](Method.GET, Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        getResponse <- userHttpService
          .run(getRequest)
          .getOrElse(fail(s"Get request was not handled"))
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
    val userHttpService: HttpService[IO] = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO])

    implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

    forAll { userSignup: SignupRequest =>
      (for {
        createRequest <- Request[IO](Method.POST, Uri.uri("/users"))
          .withBody(userSignup.asJson)
        createResponse <- userHttpService
          .run(createRequest)
          .getOrElse(fail(s"Request was not handled: $createRequest"))
        createdUser <- createResponse.as[User]
        deleteRequest = Request[IO](Method.DELETE, Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        deleteResponse <- userHttpService
          .run(deleteRequest)
          .getOrElse(fail(s"Delete request was not handled"))
        getRequest = Request[IO](Method.GET, Uri.unsafeFromString(s"/users/${createdUser.userName}"))
        getResponse <- userHttpService
          .run(getRequest)
          .getOrElse(fail(s"Get request was not handled"))
      } yield {
        createResponse.status shouldEqual Ok
        deleteResponse.status shouldEqual Ok
        getResponse.status shouldEqual NotFound
      }).unsafeRunSync
    }
  }
}

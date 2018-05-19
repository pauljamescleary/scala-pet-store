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

    val user = User("username", "firstname", "lastname", "email", "password", "phone", None)

    for {
        request <- Request[IO](Method.POST, Uri.uri("/users"))
          .withBody(user.asJson)
        response <- userHttpService
          .run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield {
        response.status shouldEqual Ok
      }
  }

  test("update user") {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService: HttpService[IO] = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO])

    implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

    val user = User("username", "firstname", "lastname", "email", "password", "phone", None)

    for {
        createRequest <- Request[IO](Method.POST, Uri.uri("/users"))
          .withBody(user.asJson)
        createResponse <- userHttpService
          .run(createRequest)
          .getOrElse(fail(s"Request was not handled: $createRequest"))
        createdUser <- createResponse.as[User]
        userToUpdate = createdUser.copy(lastName = createdUser.lastName.reverse)
        updateUser <- Request[IO](Method.PUT, Uri.unsafeFromString(s"/users/${createdUser.userName}"))
          .withBody(userToUpdate.asJson)
        updateResponse <- userHttpService
          .run(updateUser)
          .getOrElse(fail(s"Request was not handled: $updateUser"))
        updatedUser <- updateResponse.as[User]
      } yield {
        updateResponse.status shouldEqual Ok
        updatedUser.userName shouldEqual user.userName.reverse
        createdUser.id shouldEqual updatedUser.id
      }
    }

  test("get user by userName") {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService: HttpService[IO] = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO])

    implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

    val user = User("username", "firstname", "lastname", "email", "password", "phone", None)

    for {
      createRequest <- Request[IO](Method.POST, Uri.uri("/users"))
        .withBody(user.asJson)
      createResponse <- userHttpService
        .run(createRequest)
        .getOrElse(fail(s"Request was not handled: $createRequest"))
      createdUser <- createResponse.as[User]
      getResponse <- userHttpService
        .run(Request[IO](Method.GET, Uri.unsafeFromString(s"/users/${createdUser.userName}")))
        .getOrElse(fail(s"Request was not handled"))
      getUser <- getResponse.as[User]
    } yield {
      getResponse.status shouldEqual Ok
      createdUser.userName shouldEqual getUser.userName
    }

  }


  test("delete user by userName") {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val userHttpService: HttpService[IO] = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO])

    implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

    val user = User("test", "test", "test", "test", "test", "test", None)

    for {
      createRequest <- Request[IO](Method.POST, Uri.uri("/users"))
        .withBody(user.asJson)
      createResponse <- userHttpService
        .run(createRequest)
        .getOrElse(fail(s"Request was not handled: $createRequest"))
      createdUser <- createResponse.as[User]
      deleteResponse <- userHttpService
        .run(Request[IO](Method.DELETE, Uri.unsafeFromString(s"/users/${createdUser.userName}")))
        .getOrElse(fail(s"Delete request was not handled"))
      getResponse <- userHttpService
        .run(Request[IO](Method.GET, Uri.unsafeFromString(s"/users/${createdUser.userName}")))
        .getOrElse(fail(s"Get request was not handled"))
    } yield {
      createResponse.status shouldEqual Ok
      deleteResponse.status shouldEqual Ok
      getResponse.status shouldEqual NotFound
    }
  }
}

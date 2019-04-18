package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.data.Kleisli
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
import org.http4s.headers.Authorization
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration._
import tsec.authentication.{JWTAuthenticator, SecuredRequestHandler}
import tsec.mac.jca.HMACSHA256

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
 implicit val loginRequestEnc : EntityEncoder[IO, LoginRequest] = jsonEncoderOf
 implicit val loginRequestDec : EntityDecoder[IO, LoginRequest] = jsonOf

  def userHttpService(): Kleisli[IO, Request[IO], Response[IO]] = {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val key = HMACSHA256.unsafeGenerateKey
    val jwtAuth = JWTAuthenticator.unbacked.inBearerToken(1.day, None, userRepo, key)
    UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO], SecuredRequestHandler(jwtAuth)).orNotFound
  }

  def signUpAndLogIn(userSignUp: SignupRequest,
                     userEndpoint: Kleisli[IO, Request[IO], Response[IO]]
                    ): IO[(User, Option[Authorization])] =
    for {
      signUpRq <- POST(userSignUp, Uri.uri("/users"))
      signUpResp <- userEndpoint.run(signUpRq)
      user <- signUpResp.as[User]
      loginBody = LoginRequest(userSignUp.userName, userSignUp.password)
      loginRq <- POST(loginBody, Uri.uri("/login"))
      loginResp <- userEndpoint.run(loginRq)
    } yield {
      user -> loginResp.headers.get(Authorization)
    }

  def signUpAndLogInAsAdmin(userSignUp: SignupRequest,
                     userEndpoint: Kleisli[IO, Request[IO], Response[IO]]
                    ): IO[(User, Option[Authorization])] =
    signUpAndLogIn(userSignUp.copy(role = Role.Admin), userEndpoint)

  def signUpAndLogInAsCustomer(userSignUp: SignupRequest,
                            userEndpoint: Kleisli[IO, Request[IO], Response[IO]]
                           ): IO[(User, Option[Authorization])] =
    signUpAndLogIn(userSignUp.copy(role = Role.Customer), userEndpoint)


 test("create user and log in") {
   val userEndpoint = userHttpService()

   forAll { userSignup: SignupRequest =>
     val (_, authorization) = signUpAndLogIn(userSignup, userEndpoint).unsafeRunSync()
     authorization should be ('defined)
   }
 }

 test("update user") {
   val userEndpoint = userHttpService()

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
     }).unsafeRunSync
   }
 }

 test("get user by userName") {
   val userEndpoint = userHttpService()

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
     }).unsafeRunSync
   }
 }


 test("delete user by userName") {
   val userEndpoint = userHttpService()

   forAll { userSignup: SignupRequest =>
     (for {
       loginResp <- signUpAndLogInAsCustomer(userSignup, userEndpoint)
       (createdUser, Some(authorization)) = loginResp
       deleteRequest <- DELETE(Uri.unsafeFromString(s"/users/${createdUser.userName}"))
       deleteRequestAuth = deleteRequest.putHeaders(authorization)
       deleteResponse <- userEndpoint.run(deleteRequestAuth)
     } yield {
       deleteResponse.status shouldEqual Unauthorized
     }).unsafeRunSync
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
     }).unsafeRunSync
   }
 }
}

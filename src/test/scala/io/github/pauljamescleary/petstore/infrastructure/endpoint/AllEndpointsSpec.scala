package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.effect._
import domain.authentication.SignupRequest
import domain.orders.OrderService
import domain.pets.{Pet, PetService, PetValidationInterpreter}
import domain.users.{UserService, UserValidationInterpreter}
import infrastructure.endpoint.util.LoginTest
import infrastructure.repository.inmemory.{OrderRepositoryInMemoryInterpreter, PetRepositoryInMemoryInterpreter, UserRepositoryInMemoryInterpreter}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.scalatest.{FunSuite, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.http4s.implicits._
import tsec.authentication.{JWTAuthenticator, SecuredRequestHandler}
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration._
import tsec.mac.jca.{HMACSHA256, MacSigningKey}
import io.circe.generic.auto._

class AllEndpointsSpec
    extends FunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO]
    with LoginTest {

  implicit val petEnc : EntityEncoder[IO, Pet] = jsonEncoderOf
  implicit val petDec : EntityDecoder[IO, Pet] = jsonOf

  val userRepo = UserRepositoryInMemoryInterpreter[IO]()
  val userValidation = UserValidationInterpreter[IO](userRepo)
  val userService = UserService[IO](userRepo, userValidation)

  val auth = new AuthTest[IO](userRepo)
  val macKey: MacSigningKey[HMACSHA256] = HMACSHA256.unsafeGenerateKey
  val jwtAuth = JWTAuthenticator.unbacked.inBearerToken(1.day, None, userRepo, macKey)
  val usersEndpoint = UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[IO], SecuredRequestHandler(jwtAuth))

  val petRepo = PetRepositoryInMemoryInterpreter[IO]()
  val petValidation = PetValidationInterpreter[IO](petRepo)
  val petService = PetService[IO](petRepo, petValidation)
  val petEndpoint = PetEndpoints.endpoints[IO, HMACSHA256](petService, auth.securedRqHandler)

  val orderService = OrderService(OrderRepositoryInMemoryInterpreter[IO]())
  val ordersEndpoint = OrderEndpoints.endpoints[IO, HMACSHA256](orderService, auth.securedRqHandler)

  val routes = Router(
    ("/users", usersEndpoint),
    ("/pets", petEndpoint),
    ("/orders", ordersEndpoint),
  ).orNotFound

  // TODO: POST -> /pets access is forbidden
  ignore("login and post order") {

    forAll { (userSignup: SignupRequest, pet: Pet) =>
      (for {
        loginResp <- signUpAndLogInAsAdmin(userSignup, routes)
        (_, Some(authorization)) = loginResp
        createPet <- POST(pet, Uri.uri("/pets"))
        createPetAuth = createPet.putHeaders(authorization)
        resp <- routes(createPetAuth)
        _ <- resp.as[Pet]
      } yield {
        resp.status shouldEqual Ok
      }).unsafeRunSync
    }
  }
}

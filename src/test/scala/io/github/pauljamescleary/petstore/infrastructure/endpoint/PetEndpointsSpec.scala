package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import io.github.pauljamescleary.petstore.domain.pets._
import io.github.pauljamescleary.petstore.domain.users._
import io.github.pauljamescleary.petstore.PetStoreArbitraries
import io.github.pauljamescleary.petstore.infrastructure.repository.inmemory._
import domain.pets._
import infrastructure.repository.inmemory._
import cats.effect._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.mac.jca.HMACSHA256

class PetEndpointsSpec
    extends FunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO]{

  implicit val petEnc : EntityEncoder[IO, Pet] = jsonEncoderOf
  implicit val petDec : EntityDecoder[IO, Pet] = jsonOf

  test("create pet") {

    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val petRepo = PetRepositoryInMemoryInterpreter[IO]()
    val petValidation = PetValidationInterpreter[IO](petRepo)
    val petService = PetService[IO](petRepo, petValidation)
    val auth = new AuthTest[IO](userRepo)
    val petHttpService = PetEndpoints.endpoints[IO, HMACSHA256](petService, auth.securedRqHandler).orNotFound

    forAll { pet: Pet =>
      (for {
        request <- POST(pet, Uri.uri("/pets"))
        response <- petHttpService.run(request)
      } yield {
        response.status shouldEqual Unauthorized
      }).unsafeRunSync
    }

    forAll { (pet: Pet, user: User) =>
      (for {
        request <- POST(pet, Uri.uri("/pets"))
          .flatMap(auth.embedToken(user, _))
        response <- petHttpService.run(request)
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }

    forAll { (pet: Pet, user: User) =>
      (for {
        createRq <- POST(pet, Uri.uri("/pets"))
          .flatMap(auth.embedToken(user, _))
        response <- petHttpService.run(createRq)
        createdPet <- response.as[Pet]
        getRq <- GET(Uri.unsafeFromString(s"/pets/${createdPet.id.get}"))
          .flatMap(auth.embedToken(user, _))
        response2 <- petHttpService.run(getRq)
      } yield {
        response.status shouldEqual Ok
        response2.status shouldEqual Ok
      }).unsafeRunSync
    }
  }

  test("update pet") {

    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val petRepo = PetRepositoryInMemoryInterpreter[IO]()
    val petValidation = PetValidationInterpreter[IO](petRepo)
    val petService = PetService[IO](petRepo, petValidation)
    val auth = new AuthTest[IO](userRepo)
    val petHttpService = PetEndpoints.endpoints[IO, HMACSHA256](petService, auth.securedRqHandler).orNotFound

    forAll { (pet: Pet, user: AdminUser) =>
      (for {
        createRequest <- POST(pet, Uri.uri("/pets"))
          .flatMap(auth.embedToken(user.value, _))
        createResponse <- petHttpService.run(createRequest)
        createdPet <- createResponse.as[Pet]
        petToUpdate = createdPet.copy(name = createdPet.name.reverse)
        updateRequest <- PUT(petToUpdate, Uri.unsafeFromString(s"/pets/${petToUpdate.id.get}"))
          .flatMap(auth.embedToken(user.value, _))
        updateResponse <- petHttpService.run(updateRequest)
        updatedPet <- updateResponse.as[Pet]
      } yield {
        updatedPet.name shouldEqual pet.name.reverse
      }).unsafeRunSync
    }
  }
}

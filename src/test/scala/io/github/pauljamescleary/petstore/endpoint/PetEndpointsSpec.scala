package io.github.pauljamescleary.petstore
package endpoint

import cats.implicits._
import cats.effect._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.scalatest._
import org.scalatest.prop.PropertyChecks

import model._
import repository._
import service._
import validation._

class PetEndpointsSpec
    extends FunSuite
    with Matchers
    with PropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO] {

  implicit val statusDecoder: Decoder[PetStatus] = deriveEnumerationDecoder
  implicit val statusEncoder: Encoder[PetStatus] = deriveEnumerationEncoder

  test("create pet") {

    val petRepo = PetRepositoryInMemoryInterpreter[IO]()
    val petValidation = PetValidationInterpreter[IO](petRepo)
    val petService = PetService[IO](petRepo, petValidation)
    val petHttpService = PetEndpoints.endpoints[IO](petService)

    forAll { (pet: Pet) =>
      (for {
        request <- Request[IO](Method.POST, Uri.uri("/pets"))
          .withBody(pet.asJson)
        response <- petHttpService.run(request)
      } yield {
        response.orNotFound.status shouldEqual Ok
      }).unsafeRunSync
    }

  }

  test("update pet") {

    val petRepo = PetRepositoryInMemoryInterpreter[IO]()
    val petValidation = PetValidationInterpreter[IO](petRepo)
    val petService = PetService[IO](petRepo, petValidation)
    val petHttpService = PetEndpoints.endpoints[IO](petService)

    implicit val petDecoder = jsonOf[IO, Pet]

    forAll { (pet: Pet) =>
      val badPet = Pet("", "", "")

      (for {
        createRequest <- Request[IO](Method.POST, Uri.uri("/pets"))
          .withBody(pet.asJson)
        createResponse <- petHttpService.run(createRequest)
        createdPet <- createResponse.cata(_.as[Pet], badPet.pure[IO])
        petToUpdate = createdPet.copy(name = createdPet.name.reverse)
        updateRequest <- Request[IO](Method.PUT, Uri.uri("/pets"))
          .withBody(petToUpdate.asJson)
        updateResponse <- petHttpService.run(updateRequest)
        updatedPet <- updateResponse.cata(_.as[Pet], badPet.pure[IO])
      } yield {
        updatedPet.name shouldEqual pet.name.reverse
      }).unsafeRunSync
    }

  }

}

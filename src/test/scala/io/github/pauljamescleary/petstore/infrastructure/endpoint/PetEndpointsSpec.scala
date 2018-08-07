package io.github.pauljamescleary.petstore.infrastructure.endpoint

import io.github.pauljamescleary.petstore.domain.pets._
import io.github.pauljamescleary.petstore.PetStoreArbitraries
import io.github.pauljamescleary.petstore.infrastructure.repository.inmemory._
import cats.effect._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.scalatest._
import org.scalatest.prop.PropertyChecks


class PetEndpointsSpec
    extends FunSuite
    with Matchers
    with PropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO] {

  test("create pet") {

    val petRepo = PetRepositoryInMemoryInterpreter[IO]()
    val petValidation = PetValidationInterpreter[IO](petRepo)
    val petService = PetService[IO](petRepo, petValidation)
    val petHttpService = PetEndpoints.endpoints[IO](petService)

    forAll { (pet: Pet) =>
      (for {
        request <- Request[IO](Method.POST, Uri.uri("/pets"))
          .withBody(pet.asJson)
        response <- petHttpService
          .run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield {
        response.status shouldEqual Ok
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
      (for {
        createRequest <- Request[IO](Method.POST, Uri.uri("/pets"))
          .withBody(pet.asJson)
        createResponse <- petHttpService
          .run(createRequest)
          .getOrElse(fail(s"Request was not handled: $createRequest"))
        createdPet <- createResponse.as[Pet]
        petToUpdate = createdPet.copy(name = createdPet.name.reverse)
        updateRequest <- Request[IO](Method.PUT, Uri.unsafeFromString(s"/pets/${petToUpdate.id.get}"))
          .withBody(petToUpdate.asJson)
        updateResponse <- petHttpService
          .run(updateRequest)
          .getOrElse(fail(s"Request was not handled: $updateRequest"))
        updatedPet <- updateResponse.as[Pet]
      } yield {
        updatedPet.name shouldEqual pet.name.reverse
      }).unsafeRunSync
    }

  }

}

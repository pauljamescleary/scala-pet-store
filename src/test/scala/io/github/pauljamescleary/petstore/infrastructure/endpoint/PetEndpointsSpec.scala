package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.data.NonEmptyList
import domain.users._
import domain.pets._
import infrastructure.repository.inmemory._
import cats.effect._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.server.Router
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.mac.jca.HMACSHA256
import org.scalatest.matchers.should.Matchers

class PetEndpointsSpec
    extends AnyFunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {
  implicit val petEnc: EntityEncoder[IO, Pet] = jsonEncoderOf
  implicit val petDec: EntityDecoder[IO, Pet] = jsonOf

  def getTestResources(): (AuthTest[IO], HttpApp[IO], PetRepositoryInMemoryInterpreter[IO]) = {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val petRepo = PetRepositoryInMemoryInterpreter[IO]()
    val petValidation = PetValidationInterpreter[IO](petRepo)
    val petService = PetService[IO](petRepo, petValidation)
    val auth = new AuthTest[IO](userRepo)
    val petEndpoint = PetEndpoints.endpoints[IO, HMACSHA256](petService, auth.securedRqHandler)
    val petRoutes = Router(("/pets", petEndpoint)).orNotFound
    (auth, petRoutes, petRepo)
  }

  test("create pet") {
    val (auth, petRoutes, _) = getTestResources()

    forAll { pet: Pet =>
      (for {
        request <- POST(pet, uri"/pets")
        response <- petRoutes.run(request)
      } yield response.status shouldEqual Unauthorized).unsafeRunSync()
    }

    forAll { (pet: Pet, user: User) =>
      (for {
        request <- POST(pet, uri"/pets")
          .flatMap(auth.embedToken(user, _))
        response <- petRoutes.run(request)
      } yield response.status shouldEqual Ok).unsafeRunSync()
    }

    forAll { (pet: Pet, user: User) =>
      (for {
        createRq <- POST(pet, uri"/pets")
          .flatMap(auth.embedToken(user, _))
        response <- petRoutes.run(createRq)
        createdPet <- response.as[Pet]
        getRq <- GET(Uri.unsafeFromString(s"/pets/${createdPet.id.get}"))
          .flatMap(auth.embedToken(user, _))
        response2 <- petRoutes.run(getRq)
      } yield {
        response.status shouldEqual Ok
        response2.status shouldEqual Ok
      }).unsafeRunSync()
    }
  }

  test("update pet") {
    val (auth, petRoutes, _) = getTestResources()

    forAll { (pet: Pet, user: AdminUser) =>
      (for {
        createRequest <- POST(pet, uri"/pets")
          .flatMap(auth.embedToken(user.value, _))
        createResponse <- petRoutes.run(createRequest)
        createdPet <- createResponse.as[Pet]
        petToUpdate = createdPet.copy(name = createdPet.name.reverse)
        updateRequest <- PUT(petToUpdate, Uri.unsafeFromString(s"/pets/${petToUpdate.id.get}"))
          .flatMap(auth.embedToken(user.value, _))
        updateResponse <- petRoutes.run(updateRequest)
        updatedPet <- updateResponse.as[Pet]
      } yield updatedPet.name shouldEqual pet.name.reverse).unsafeRunSync()
    }
  }

  test("find by tag") {
    val (auth, petRoutes, petRepo) = getTestResources()

    forAll { (pet: Pet, user: AdminUser) =>
      (for {
        createRequest <- POST(pet, uri"/pets")
          .flatMap(auth.embedToken(user.value, _))
        createResponse <- petRoutes.run(createRequest)
        createdPet <- createResponse.as[Pet]
      } yield createdPet.tags.toList.headOption match {
        case Some(tag) =>
          val petsFoundByTag = petRepo.findByTag(NonEmptyList.of(tag)).unsafeRunSync()
          petsFoundByTag.contains(createdPet) shouldEqual true
        case _ => ()
      }).unsafeRunSync()
    }
  }
}

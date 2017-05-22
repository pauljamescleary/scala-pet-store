package io.github.pauljamescleary.petstore

import fs2.Task
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.http4s.{HttpService, QueryParamDecoder}
import org.http4s.circe._
import org.http4s.dsl._

import scala.language.higherKinds

object PetEndpoints {

  /* Necessary for decoding query parameters */
  import QueryParamDecoder._

  /* Needed for service composition via |+| */
  import cats.implicits._

  /* Parses out the id query param */
  object IdMatcher extends QueryParamDecoderMatcher[Long]("id")

  /* This is necessary as circe does not do auto derivation for ADTs */
  implicit private val decodePetType = deriveEnumerationDecoder[PetType]
  implicit private val encodePetType = deriveEnumerationEncoder[PetType]

  /* This is necessary as circe defaults options to null */
  implicit private val encodePet = deriveEncoder[Pet].mapJsonObject {
    _.filter {
      case ("id", value) => !value.isNull
      case _ => true
    }
  }
  implicit private val decodePet = deriveDecoder[Pet].map {
    case fix@Pet(_, _, _, null) => fix.copy(id = None)
    case ok => ok
  }

  private def createPetEndpoint(petService: PetService[Task]): HttpService = HttpService {
    case req@POST -> Root / "pet" => {
      for {
        pet <- req.as(jsonOf[Pet])
        saved <- petService.create(pet)
        resp <- Ok(saved.asJson)
      } yield resp
    }.handleWith {
      case PetAlreadyExistsError(pet) => Conflict(s"The pet ${pet.name} of type ${pet.typ} already exists")
    }
  }

  private def getPetEndpoint(petService: PetService[Task]): HttpService = HttpService {
    case GET -> Root / "pet" :? IdMatcher(id) => {
      for {
        retrieved <- petService.get(id)
        resp <- Ok(retrieved.asJson)
      } yield resp
    }.handleWith {
      case PetNotFoundError(notFound) => NotFound(s"The pet with id $notFound was not found")
    }
  }

  private def deletePetEndpoint(petService: PetService[Task]): HttpService = HttpService {
    case DELETE -> Root / "pet" :? IdMatcher(id) =>
      for {
        _ <- petService.delete(id)
        resp <- Ok()
      } yield resp
  }

  def endpoints(petService: PetService[Task]): HttpService =
    createPetEndpoint(petService) |+| getPetEndpoint(petService) |+| deletePetEndpoint(petService)
}

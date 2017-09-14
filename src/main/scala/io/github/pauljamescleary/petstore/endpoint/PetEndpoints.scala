package io.github.pauljamescleary.petstore.endpoint

import cats.data.Validated.Valid
import cats._, cats.syntax._, cats.instances._, cats.data._, cats.effect.IO, cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.model.{Pet, Status}
import io.github.pauljamescleary.petstore.service.PetService
import io.github.pauljamescleary.petstore.validation.{PetAlreadyExistsError, PetNotFoundError}
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.{HttpService, QueryParamDecoder}

import scala.language.higherKinds

object PetEndpoints {

  /* Necessary for decoding query parameters */
  import QueryParamDecoder._

  /* Needed for service composition via |+| */
  import cats.implicits._

  /* Parses out the id query param */
  object IdMatcher extends QueryParamDecoderMatcher[Long]("id")

  /* Parses out the offset and page size params */
  object PageSizeMatcher extends QueryParamDecoderMatcher[Int]("pageSize")
  object OffsetMatcher extends QueryParamDecoderMatcher[Int]("offset")

  /* Parses out status query param which could be multi param */
  implicit val statusQueryParamDecoder: QueryParamDecoder[Status] =
    QueryParamDecoder[String].map(Status.apply)

  /* Relies on the statusQueryParamDecoder implicit, will parse out a possible multi-value query parameter */
  object StatusMatcher extends OptionalMultiQueryParamDecoderMatcher[Status]("status")

  /* We need to define an enum encoder and decoder since these do not come out of the box with generic derivation */
  implicit val statusDecoder = deriveEnumerationDecoder[Status]
  implicit val statusEncoder = deriveEnumerationEncoder[Status]

  private def createPetEndpoint(petService: PetService[IO]): HttpService[IO] = HttpService[IO] {
    case req@POST -> Root / "pets" =>
      val action = for {
        pet <- req.as(implicitly, jsonOf[IO, Pet]) // <-- TODO: Make this cleaner in HTTP4S
        result <- petService.create(pet).value
      } yield result

      action.flatMap {
        case Right(saved) =>
          Ok(saved.asJson)
        case Left(PetAlreadyExistsError(existing)) =>
          Conflict(s"The pet ${existing.name} of category ${existing.category} already exists")
      }
  }

  private def updatePetEndpoint(petService: PetService[IO]): HttpService[IO] = HttpService[IO] {
    case req@PUT -> Root / "pets" =>
      val action = for {
        pet <- req.as(implicitly, jsonOf[IO, Pet]) // <-- TODO: Make this cleaner in HTTP4S
        result <- petService.update(pet).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(PetNotFoundError) => NotFound("The pet was not found")
      }
  }

  private def getPetEndpoint(petService: PetService[IO]): HttpService[IO] = HttpService[IO] {
    case GET -> Root / "pets" :? IdMatcher(id) =>
      petService.get(id).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(PetNotFoundError) => NotFound("The pet was not found")
      }
  }

  private def deletePetEndpoint(petService: PetService[IO]): HttpService[IO] = HttpService[IO] {
    case DELETE -> Root / "pets" :? IdMatcher(id) =>
      for {
        _ <- petService.delete(id)
        resp <- Ok()
      } yield resp
  }

  private def listPetsEndpoint(petService: PetService[IO]): HttpService[IO] = HttpService[IO] {
    case GET -> Root / "pets" :? PageSizeMatcher(pageSize) :? OffsetMatcher(offset) =>
      for {
        retrieved <- petService.list(pageSize, offset)
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  private def findPetsByStatusEndpoint(petService: PetService[IO]): HttpService[IO] = HttpService[IO] {
    case GET -> Root / "pets" / "findByStatus" :? StatusMatcher(Valid(Nil)) =>
      // User did not specify any statuses
      BadRequest("status parameter not specified")

    case GET -> Root / "pets" / "findByStatus" :? StatusMatcher(Valid(statuses)) =>
      // We have a list of valid statuses, find them and return
      for {
        retrieved <- petService.findByStatus(NonEmptyList.fromListUnsafe(statuses))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  def endpoints(petService: PetService[IO]): HttpService[IO] =
    createPetEndpoint(petService) |+|
      getPetEndpoint(petService) |+|
      deletePetEndpoint(petService) |+|
      listPetsEndpoint(petService) |+|
      findPetsByStatusEndpoint(petService) |+|
      updatePetEndpoint(petService)
}

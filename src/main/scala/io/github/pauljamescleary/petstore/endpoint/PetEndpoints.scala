package io.github.pauljamescleary.petstore.endpoint

import fs2.Task
import io.circe._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.model.{Available, Pet, Status}
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

  implicit val longOptionEncoder = Encoder.encodeOption[Long]

  implicit val encodePet: Encoder[Pet] = new Encoder[Pet] {
    final def apply(a: Pet): Json = Json.obj(
      ("name", Json.fromString(a.name)),
      ("category", Json.fromString(a.category)),
      ("bio", Json.fromString(a.bio)),
      ("status", Json.fromString(Status.nameOf(a.status))),
      ("tags", Json.fromValues(a.tags.map(Json.fromString))),
      ("photoUrls", Json.fromValues(a.photoUrls.map(Json.fromString))),
      ("id", a.id.map(Json.fromLong).getOrElse(Json.Null))
    )
  }

  implicit val setStringOptionDecoder: Decoder[Set[String]] = Decoder.decodeOption[Set[String]].map {
    case Some(ss) => ss
    case None => Set.empty[String]
  }

  implicit val decodePet: Decoder[Pet] = new Decoder[Pet] {
    final def apply(c: HCursor): Decoder.Result[Pet] =
      for {
        name <- c.get[String]("name")
        category <- c.get[String]("category")
        bio <- c.get[String]("bio")
        status <- c.get[Option[String]]("status").map { case Some(s) => Status.apply(s); case None => Available }
        tags <- c.get[Set[String]]("tags")
        photoUrls <- c.get[Set[String]]("photoUrls")
        id <- c.get[Option[Long]]("id")
      } yield {
        Pet(name, category, bio, status, tags, photoUrls, id)
      }
  }

  private def createPetEndpoint(petService: PetService[Task]): HttpService = HttpService {
    case req@POST -> Root / "pets" => {
      for {
        pet <- req.as(jsonOf[Pet])
        saved <- petService.create(pet)
        resp <- Ok(saved.asJson)
      } yield resp
    }.handleWith {
      case PetAlreadyExistsError(pet) => Conflict(s"The pet ${pet.name} of category ${pet.category} already exists")
    }
  }

  private def getPetEndpoint(petService: PetService[Task]): HttpService = HttpService {
    case GET -> Root / "pets" :? IdMatcher(id) => {
      for {
        retrieved <- petService.get(id)
        resp <- Ok(retrieved.asJson)
      } yield resp
    }.handleWith {
      case PetNotFoundError(notFound) => NotFound(s"The pet with id $notFound was not found")
    }
  }

  private def deletePetEndpoint(petService: PetService[Task]): HttpService = HttpService {
    case DELETE -> Root / "pets" :? IdMatcher(id) =>
      for {
        _ <- petService.delete(id)
        resp <- Ok()
      } yield resp
  }

  private def listPetsEndpoint(petService: PetService[Task]): HttpService = HttpService {
    case GET -> Root / "pets" :? PageSizeMatcher(pageSize) :? OffsetMatcher(offset) =>
      for {
        retrieved <- petService.list(pageSize, offset)
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  def endpoints(petService: PetService[Task]): HttpService =
    createPetEndpoint(petService) |+| getPetEndpoint(petService) |+| deletePetEndpoint(petService) |+| listPetsEndpoint(petService)
}

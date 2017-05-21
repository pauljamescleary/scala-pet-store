package io.github.pauljamescleary.petstore

import fs2.Task
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.{->, /, Ok, POST, Root, _}

import scala.language.higherKinds

object PetEndpoints {

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
    case fix @ Pet(_, _, _, null) => fix.copy(id = None)
    case ok => ok
  }

  // TODO: This is clunky, right now we must assert that our effect type is Task because that is what HTTP4s uses
  def endpoints(petService: PetService[Task]) = HttpService {
    case req @ POST -> Root / "pet" =>
      for {
        pet <- req.as(jsonOf[Pet])
        saved <- petService.create(pet)
        resp <- Ok(saved.asJson)
      } yield resp
  }
}

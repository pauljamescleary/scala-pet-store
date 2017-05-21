package io.github.pauljamescleary.petstore

import io.circe._
import io.circe.generic.extras.semiauto.deriveEnumerationDecoder
import io.circe.generic.extras.semiauto.deriveEnumerationEncoder
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.Model._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.HttpService
import org.http4s.dsl.{->, /, Ok, POST, Root}

import scala.util.Random

object PetService {

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

  private val random = new Random()

  val service = HttpService {
    case req @ POST -> Root / "pet" =>
      for {
        pet <- req.as(jsonOf[Pet])
        resp <- Ok(pet.copy(id=Some(random.nextLong)).asJson)
      } yield resp
  }
}

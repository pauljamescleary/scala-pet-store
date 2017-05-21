package io.github.pauljamescleary.petstore

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.Model._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.HttpService
import org.http4s.dsl.{->, /, Ok, POST, Root}

object PetService {

  /* This is necessary as circe does not do auto derivation for ADTs */
  implicit val encodePetType: Encoder[PetType] = {
    case Dog => Json.fromString("Dog")
    case Cat => Json.fromString("Cat")
    case Hamster => Json.fromString("Hamster")
    case HedgeHog => Json.fromString("HedgeHog")
    case Chinchillas => Json.fromString("Chinchillas")
  }

  implicit val decodePetType: Decoder[PetType] = Decoder.instance {
    hCursor =>
      hCursor.as[String] map {
        case "Dog" => Dog
        case "Cat" => Cat
        case "Hamster" => Hamster
        case "HedgeHog" => HedgeHog
        case "Chinchillas" => Chinchillas
      }
  }

  val service = HttpService {
    case req @ POST -> Root / "pet" =>
      for {
        pet <- req.as(jsonOf[Pet])
        resp <- Ok(pet.copy(id = Some(1)).asJson)
      } yield resp
  }
}

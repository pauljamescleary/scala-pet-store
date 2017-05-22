package io.github.pauljamescleary.petstore

package object model {

  sealed trait PetType
  case object Dog extends PetType
  case object Cat extends PetType
  case object Hamster extends PetType
  case object HedgeHog extends PetType
  case object Chinchillas extends PetType
  object PetType {
    def apply(name: String): PetType = name match {
      case "Dog" => Dog
      case "Cat" => Cat
      case "Hamster" => Hamster
      case "HedgeHog" => HedgeHog
      case "Chinchillas" => Chinchillas
    }
    def nameOf(typ: PetType): String = typ match {
      case Dog => "Dog"
      case Cat => "Cat"
      case Hamster => "Hamster"
      case HedgeHog => "HedgeHog"
      case Chinchillas => "Chinchillas"
    }
  }

  case class Pet(name: String, typ: PetType, bio: String, id: Option[Long])
}

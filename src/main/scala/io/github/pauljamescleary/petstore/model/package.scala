package io.github.pauljamescleary.petstore

package object model {

  sealed trait PetType
  case object Dog extends PetType
  case object Cat extends PetType
  case object Hamster extends PetType
  case object HedgeHog extends PetType
  case object Chinchillas extends PetType

  case class Pet(name: String, typ: PetType, bio: String, id: Option[Long])
}

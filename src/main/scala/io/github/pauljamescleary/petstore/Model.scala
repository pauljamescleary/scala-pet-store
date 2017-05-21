package io.github.pauljamescleary.petstore

object Model {

  sealed trait PetType
  case object Dog extends PetType
  case object Cat extends PetType
  case object Hamster extends PetType
  case object HedgeHog extends PetType
  case object Chinchillas extends PetType

  case class Pet(name: String, typ: PetType, image: Option[Array[Byte]], bio: String, id: Option[Long])

}

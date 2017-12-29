package io.github.pauljamescleary.petstore.domain.validation

import io.github.pauljamescleary.petstore.domain.model.{Pet, User}

sealed trait ValidationError extends Product with Serializable
case class PetAlreadyExistsError(pet: Pet) extends ValidationError
case object PetNotFoundError extends ValidationError
case object OrderNotFoundError extends ValidationError
case object UserNotFoundError extends ValidationError
case class UserAlreadyExistsError(user: User) extends ValidationError

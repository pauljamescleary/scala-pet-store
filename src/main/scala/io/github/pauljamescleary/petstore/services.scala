package io.github.pauljamescleary.petstore

import fs2.util.Monad

import scala.language.higherKinds

/**
  *
  * @param ev$1
  * @param repository where we get our data
  * @param validation something that provides validations to the service
  * @tparam F - this is the container for the things we work with, could be scala.concurrent.Future, Option, anything
  *           as long as it is a Monad
  */
class PetService[F[_] : Monad](implicit repository: PetRepositoryAlgebra[F], validation: PetValidationAlgebra[F]) {
  import fs2.util.syntax._

  def create(pet: Pet): F[Pet] = {
    for {
      _ <- validation.doesNotExist(pet)
      saved <- repository.put(pet)
    } yield saved
  }

  def get(id: Long): F[Option[Pet]] = repository.get(id)
}
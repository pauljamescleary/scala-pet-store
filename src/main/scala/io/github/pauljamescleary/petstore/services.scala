package io.github.pauljamescleary.petstore

import fs2.util.Monad

import scala.language.higherKinds

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
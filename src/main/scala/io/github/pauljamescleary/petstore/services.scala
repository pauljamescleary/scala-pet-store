package io.github.pauljamescleary.petstore

import fs2.util.Monad

import scala.language.higherKinds

class PetService[F[_] : Monad](repository: PetRepositoryAlgebra[F]) {

  def create(pet: Pet): F[Pet] = repository.put(pet)

  def get(id: Long): F[Option[Pet]] = repository.get(id)
}
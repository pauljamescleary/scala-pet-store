package io.github.pauljamescleary.petstore

import scala.language.higherKinds

trait PetRepositoryAlgebra[F[_]] {

  def put(pet: Pet): F[Pet]

  def get(id: Long): F[Option[Pet]]
}

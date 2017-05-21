package io.github.pauljamescleary.petstore

import scala.language.higherKinds

trait PetRepositoryAlgebra[F[_]] {

  def put(pet: Pet): F[Pet]

  def get(id: Long): F[Option[Pet]]

  def findByNameAndType(name: String, typ: PetType): F[Set[Pet]]
}

case class PetAlreadyExistsError(pet: Pet) extends Throwable

trait PetValidationAlgebra[F[_]] {

  /* Fails with a PetAlreadyExistsError */
  def doesNotExist(pet: Pet): F[Unit]
}

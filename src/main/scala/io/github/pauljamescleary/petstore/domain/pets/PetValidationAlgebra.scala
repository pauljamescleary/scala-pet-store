package io.github.pauljamescleary.petstore.domain
package pets

trait PetValidationAlgebra[F[_]] {
  /* Fails with a PetAlreadyExistsError */
  def doesNotExist(pet: Pet): F[Either[PetAlreadyExistsError, Unit]]

  /* Fails with a PetNotFoundError if the pet id does not exist or if it is none */
  def exists(petId: Option[Long]): F[Either[PetNotFoundError.type, Unit]]
}

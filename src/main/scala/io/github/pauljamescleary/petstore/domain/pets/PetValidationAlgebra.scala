package io.github.pauljamescleary.petstore.domain
package pets

import cats.data.EitherT

trait PetValidationAlgebra[F[_]] {
  /* Fails with a PetAlreadyExistsError */
  def doesNotExist(pet: Pet): EitherT[F, PetAlreadyExistsError, Unit]

  /* Fails with a PetNotFoundError if the pet id does not exist or if it is none */
  def exists(petId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit]
}

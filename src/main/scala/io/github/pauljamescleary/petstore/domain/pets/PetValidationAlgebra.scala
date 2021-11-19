package io.github.pauljamescleary.petstore.domain
package pets

import cats.effect.IO

trait PetValidationAlgebra {
  /* Fails with a PetAlreadyExistsError */
  def doesNotExist(pet: Pet): IO[Either[PetAlreadyExistsError, Unit]]

  /* Fails with a PetNotFoundError if the pet id does not exist or if it is none */
  def exists(petId: Option[Long]): IO[Either[PetNotFoundError.type, Unit]]
}

package io.github.pauljamescleary.petstore.domain.pets

import scala.language.higherKinds
import cats.data.EitherT
import cats.effect.Bracket
import io.github.pauljamescleary.petstore.domain.{PetAlreadyExistsError, PetNotFoundError}

trait PetValidationAlgebra[F[_]] {
  type B = Bracket[F, Throwable]

  /* Fails with a PetAlreadyExistsError */
  def doesNotExist(pet: Pet)(implicit b: B): EitherT[F, PetAlreadyExistsError, Unit]

  /* Fails with a PetNotFoundError if the pet id does not exist or if it is none */
  def exists(petId: Option[Long])(implicit b: B): EitherT[F, PetNotFoundError.type, Unit]
}

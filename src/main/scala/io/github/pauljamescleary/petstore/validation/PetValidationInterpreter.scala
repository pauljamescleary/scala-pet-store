package io.github.pauljamescleary.petstore.validation

import cats.data.EitherT
import cats.effect.IO
import io.github.pauljamescleary.petstore.model.Pet
import io.github.pauljamescleary.petstore.repository.PetRepositoryAlgebra

class PetValidationInterpreter(implicit repository: PetRepositoryAlgebra[IO]) extends PetValidationAlgebra[IO] {

  def doesNotExist(pet: Pet): EitherT[IO, ValidationError, Unit] = EitherT {
    repository.findByNameAndCategory(pet.name, pet.category).map { matches =>
      if (matches.forall(possibleMatch => possibleMatch.bio != pet.bio)) {
        Right(())
      } else {
        Left(PetAlreadyExistsError(pet))
      }
    }
  }

  def exists(petId: Option[Long]): EitherT[IO, ValidationError, Unit] = EitherT {
    petId match {
      case Some(id) =>
        // Ensure is a little tough to follow, it says "make sure this condition is true, otherwise throw the error specified
        // In this example, we make sure that the option returned has a value, otherwise the pet was not found
        repository.get(id).map {
          case Some(_) => Right()
          case _ => Left(PetNotFoundError)
        }

      case _ =>
        IO.pure(Left(PetNotFoundError))
    }
  }
}

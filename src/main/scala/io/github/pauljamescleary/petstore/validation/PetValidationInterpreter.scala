package io.github.pauljamescleary.petstore.validation

import cats.effect.IO
import io.github.pauljamescleary.petstore.model.Pet
import io.github.pauljamescleary.petstore.repository.PetRepositoryAlgebra

class PetValidationInterpreter(implicit repository: PetRepositoryAlgebra[IO]) extends PetValidationAlgebra[IO] {
  import cats.syntax.monadError._

  def doesNotExist(pet: Pet): IO[Unit] = {
    repository.findByNameAndCategory(pet.name, pet.category).ensure(PetAlreadyExistsError(pet)) { matches =>
      matches.forall(possibleMatch => possibleMatch.bio != pet.bio)
    }.map(_ => ())
  }

  def exists(petId: Option[Long]): IO[Unit] = {
    petId match {
      case Some(id) =>
        // Ensure is a little tough to follow, it says "make sure this condition is true, otherwise throw the error specified
        // In this example, we make sure that the option returned has a value, otherwise the pet was not found
        repository.get(id).ensure(PetNotFoundError(id)) {_.isDefined }.map(_ => ())
      case _ =>
        IO.raiseError(PetNotFoundError(0))
    }
  }
}

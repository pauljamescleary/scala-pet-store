package io.github.pauljamescleary.petstore.validation

import cats.effect.IO
import io.github.pauljamescleary.petstore.model.Pet
import io.github.pauljamescleary.petstore.repository.PetRepositoryAlgebra

class PetValidationTaskInterpreter(implicit repository: PetRepositoryAlgebra[IO]) extends PetValidationAlgebra[IO] {
  import cats.syntax.monadError._

  def doesNotExist(pet: Pet): IO[Unit] = {
    repository.findByNameAndCategory(pet.name, pet.category).ensure(PetAlreadyExistsError(pet)) { matches =>
      matches.forall(possibleMatch => possibleMatch.bio != pet.bio)
    }.map(_ => ())
  }
}

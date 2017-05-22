package io.github.pauljamescleary.petstore.validation

import fs2.Task
import io.github.pauljamescleary.petstore.model.Pet
import io.github.pauljamescleary.petstore.repository.PetRepositoryAlgebra

class PetValidationTaskInterpreter(implicit repository: PetRepositoryAlgebra[Task]) extends PetValidationAlgebra[Task] {

  def doesNotExist(pet: Pet): Task[Unit] = {
    repository.findByNameAndType(pet.name, pet.typ).ensure(PetAlreadyExistsError(pet)) { matches =>
      matches.forall(possibleMatch => possibleMatch.bio != pet.bio)
    }.map(_ => ())
  }
}

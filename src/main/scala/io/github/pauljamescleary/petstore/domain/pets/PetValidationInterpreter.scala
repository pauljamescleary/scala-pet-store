package io.github.pauljamescleary.petstore.domain
package pets

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._

class PetValidationInterpreter[F[_]: Applicative](repository: PetRepositoryAlgebra[F])
    extends PetValidationAlgebra[F] {
  def doesNotExist(pet: Pet): EitherT[F, PetAlreadyExistsError, Unit] = EitherT {
    repository.findByNameAndCategory(pet.name, pet.category).map { matches =>
      if (matches.forall(possibleMatch => possibleMatch.bio != pet.bio)) {
        Right(())
      } else {
        Left(PetAlreadyExistsError(pet))
      }
    }
  }

  def exists(petId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit] =
    EitherT {
      petId match {
        case Some(id) =>
          // Ensure is a little tough to follow, it says "make sure this condition is true, otherwise throw the error specified
          // In this example, we make sure that the option returned has a value, otherwise the pet was not found
          repository.get(id).map {
            case Some(_) => Right(())
            case _ => Left(PetNotFoundError)
          }
        case _ =>
          Either.left[PetNotFoundError.type, Unit](PetNotFoundError).pure[F]
      }
    }
}

object PetValidationInterpreter {
  def apply[F[_]: Applicative](repository: PetRepositoryAlgebra[F]) =
    new PetValidationInterpreter[F](repository)
}

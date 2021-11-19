package io.github.pauljamescleary.petstore.domain
package pets

import cats.Applicative
import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all._

class PetValidationInterpreter(repository: PetRepositoryAlgebra)
    extends PetValidationAlgebra {
  def doesNotExist(pet: Pet): IO[Either[PetAlreadyExistsError, Unit]] = EitherT {
    repository.findByNameAndCategory(pet.name, pet.category).map { matches =>
      if (matches.forall(possibleMatch => possibleMatch.bio != pet.bio)) {
        Right(())
      } else {
        Left(PetAlreadyExistsError(pet))
      }
    }
  }.value

  def exists(petId: Option[Long]): IO[Either[PetNotFoundError.type, Unit]] =
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
          PetNotFoundError.asLeft[Unit].pure[IO]
      }
    }.value
}

object PetValidationInterpreter {
  def apply[F[_]: Applicative](repository: PetRepositoryAlgebra) =
    new PetValidationInterpreter(repository)
}

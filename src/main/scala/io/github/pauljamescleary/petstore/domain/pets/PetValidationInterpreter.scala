package io.github.pauljamescleary.petstore.domain
package pets

import cats.Applicative
import cats.syntax.functor._
import cats.syntax.either._
import cats.syntax.applicative._

class PetValidationInterpreter[F[_]: Applicative](repository: PetRepositoryAlgebra[F])
    extends PetValidationAlgebra[F] {

  override def doesNotExist(pet: Pet): F[Either[PetAlreadyExistsError, Unit]] =
    repository.findByNameAndCategory(pet.name, pet.category).map { pets =>
      val petBios = pets.map(_.bio)

      Either.cond(!petNames.contains(pet.bio), (), PetAlreadyExistsError(pet))
    }

  override def exists(petId: Option[Long]): F[Either[PetNotFoundError.type, Unit]] = {
    petId.fold(PetNotFoundError.asLeft[Unit].pure[F]) { id =>
      repository.get(id).map { maybePet =>
        Either.cond(maybePet.isDefined, (), PetNotFoundError)
      }
    }
  }
}

object PetValidationInterpreter {
  def apply[F[_]: Applicative](repository: PetRepositoryAlgebra[F]) =
    new PetValidationInterpreter[F](repository)
}

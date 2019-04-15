package io.github.pauljamescleary.petstore.domain
package pets

import scala.language.higherKinds

import cats._
import cats.data._

/**
  * The entry point to our domain, works with repositories and validations to implement behavior
  * @param repository where we get our data
  * @param validation something that provides validations to the service
  * @tparam F - this is the container for the things we work with, could be scala.concurrent.Future, Option, anything
  *           as long as it is a Monad
  */
class PetService[F[_]](repository: PetRepositoryAlgebra[F], validation: PetValidationAlgebra[F]) {
  import cats.syntax.all._

  def create(pet: Pet)(implicit M: Monad[F]): EitherT[F, PetAlreadyExistsError, Pet] = for {
    _ <- validation.doesNotExist(pet)
    saved <- EitherT.liftF(repository.create(pet))
  } yield saved

  /* Could argue that we could make this idempotent on put and not check if the pet exists */
  def update(pet: Pet)(implicit M: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] = for {
    _ <- validation.exists(pet.id)
    saved <- EitherT.fromOptionF(repository.update(pet), PetNotFoundError)
  } yield saved

  def get(id: Long)(implicit M: Monad[F]): EitherT[F, PetNotFoundError.type, Pet] =
    EitherT.fromOptionF(repository.get(id), PetNotFoundError)

  /* In some circumstances we may care if we actually delete the pet; here we are idempotent and do not care */
  def delete(id: Long)(implicit M: Monad[F]): F[Unit] =
    repository.delete(id).as(())

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    repository.list(pageSize, offset)

  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    repository.findByStatus(statuses)

  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    repository.findByTag(tags)
}

object PetService {
  def apply[F[_]: Monad](repository: PetRepositoryAlgebra[F], validation: PetValidationAlgebra[F]) =
    new PetService[F](repository, validation)
}

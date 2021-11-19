package io.github.pauljamescleary.petstore.domain
package pets

import cats.Functor
import cats.data._
import cats.Monad
import cats.effect.IO
import cats.syntax.all._

/**
  * The entry point to our domain, works with repositories and validations to implement behavior
  * @param repository where we get our data
  * @param validation something that provides validations to the service
  */
class PetService(
    repository: PetRepositoryAlgebra,
    validation: PetValidationAlgebra,
) {
  def create(pet: Pet): IO[Either[PetAlreadyExistsError, Pet]] =
    (for {
      _ <- EitherT(validation.doesNotExist(pet))
      saved <- EitherT.liftF(repository.create(pet))
    } yield saved).value

  /* Could argue that we could make this idempotent on put and not check if the pet exists */
  def update(pet: Pet): IO[Either[PetNotFoundError.type, Pet]] =
    (for {
      _ <- EitherT(validation.exists(pet.id))
      saved <- EitherT.fromOptionF(repository.update(pet), PetNotFoundError)
    } yield saved).value

  def get(id: Long): IO[Either[PetNotFoundError.type, Pet]] =
    EitherT.fromOptionF(repository.get(id), PetNotFoundError).value

  /* In some circumstances we may care if we actually delete the pet; here we are idempotent and do not care */
  def delete(id: Long): IO[Unit] =
    repository.delete(id).as(())

  def list(pageSize: Int, offset: Int): IO[List[Pet]] =
    repository.list(pageSize, offset)

  def findByStatus(statuses: NonEmptyList[PetStatus]): IO[List[Pet]] =
    repository.findByStatus(statuses)

  def findByTag(tags: NonEmptyList[String]): IO[List[Pet]] =
    repository.findByTag(tags)
}

object PetService {
  def apply(
      repository: PetRepositoryAlgebra,
      validation: PetValidationAlgebra,
  ): PetService =
    new PetService(repository, validation)
}

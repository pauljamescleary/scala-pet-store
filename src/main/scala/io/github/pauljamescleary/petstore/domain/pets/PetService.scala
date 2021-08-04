package io.github.pauljamescleary.petstore.domain
package pets

import cats.data._
import cats.Monad
import cats.syntax.all._
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/** The entry point to our domain, works with repositories and validations to implement behavior
  * @param repository where we get our data
  * @param validation something that provides validations to the service
  * @tparam F - this is the container for the things we work with, could be scala.concurrent.Future, Option, anything
  *           as long as it is a Monad
  */
class PetService[F[_]: Logger: Monad](
    repository: PetRepositoryAlgebra[F],
    validation: PetValidationAlgebra[F],
) {
  def create(pet: Pet): EitherT[F, PetAlreadyExistsError, Pet] =
    for {
      _ <- EitherT.liftF(Logger[F].info(s"Creating pet $pet"))
      _ <- validation.doesNotExist(pet)
      saved <- EitherT.liftF(repository.create(pet))
    } yield saved

  /* Could argue that we could make this idempotent on put and not check if the pet exists */
  def update(pet: Pet): EitherT[F, PetNotFoundError.type, Pet] =
    for {
      _ <- EitherT.liftF(Logger[F].info(s"Updating pet $pet"))
      _ <- validation.exists(pet.id)
      saved <- EitherT.fromOptionF(repository.update(pet), PetNotFoundError)
    } yield saved

  def get(id: Long): EitherT[F, PetNotFoundError.type, Pet] =
    EitherT.fromOptionF(
      Logger[F].info(s"Getting pet with id $id") *> repository.get(id),
      PetNotFoundError,
    )

  /* In some circumstances we may care if we actually delete the pet; here we are idempotent and do not care */
  def delete(id: Long): F[Unit] =
    repository.delete(id).as(())

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    repository.list(pageSize, offset)

  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    repository.findByStatus(statuses)

  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    repository.findByTag(tags)
}

object PetService {
  def apply[F[_]: Sync](
      repository: PetRepositoryAlgebra[F],
      validation: PetValidationAlgebra[F],
  ): Resource[F, PetService[F]] =
    Resource.eval(Slf4jLogger.create[F]).map { implicit logger: Logger[F] =>
      new PetService[F](repository, validation)
    }
}

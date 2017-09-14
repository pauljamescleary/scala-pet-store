package io.github.pauljamescleary.petstore.service

import cats._
import cats.data._
import cats.effect.IO
import cats.implicits._
import cats.instances._
import cats.syntax._
import io.github.pauljamescleary.petstore.model.{Pet, Status}
import io.github.pauljamescleary.petstore.repository.PetRepositoryAlgebra
import io.github.pauljamescleary.petstore.validation.{PetNotFoundError, PetValidationAlgebra, ValidationError}

import scala.language.higherKinds

/**
  *
  * @param repository where we get our data
  * @param validation something that provides validations to the service
  * @tparam F - this is the container for the things we work with, could be scala.concurrent.Future, Option, anything
  *           as long as it is a Monad
  */
class PetService[F[_]](implicit repository: PetRepositoryAlgebra[F], validation: PetValidationAlgebra[F]) {
  import cats.syntax.all._

  private implicit class FImprovements[A](f: F[A]) {
    def liftT(implicit M: Monad[F]): EitherT[F, ValidationError, A] = EitherT.liftT(f)
  }

  def create(pet: Pet)(implicit M: Monad[F]): EitherT[F, ValidationError, Pet] = {
    for {
      _ <- validation.doesNotExist(pet)
      saved <- repository.put(pet).liftT
    } yield saved
  }

  /* Could argue that we could make this idempotent on put and not check if the pet exists */
  def update(pet: Pet)(implicit M: Monad[F]): EitherT[F, ValidationError, Pet] = {
    for {
      _ <- validation.exists(pet.id)
      saved <- repository.put(pet).liftT
    } yield saved
  }

  def get(id: Long)(implicit M: Monad[F]): EitherT[F, ValidationError, Pet] = EitherT {
    repository.get(id).map {
      case None => Left(PetNotFoundError)
      case Some(found) => Right(found)
    }
  }

  /* In some circumstances we may care if we actually delete the pet; here we are idempotent and do not care */
  def delete(id: Long)(implicit M: Monad[F]): F[Unit] = repository.delete(id).map(_ => ())

  def list(pageSize: Int, offset: Int): F[Seq[Pet]] = repository.list(pageSize, offset)

  def findByStatus(statuses: NonEmptyList[Status]): F[Seq[Pet]] = repository.findByStatus(statuses)
}
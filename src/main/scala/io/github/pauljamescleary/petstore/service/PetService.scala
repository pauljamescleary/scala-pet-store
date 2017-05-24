package io.github.pauljamescleary.petstore.service

import fs2.util.{Catchable, Monad}
import io.github.pauljamescleary.petstore.model.Pet
import io.github.pauljamescleary.petstore.repository.PetRepositoryAlgebra
import io.github.pauljamescleary.petstore.validation.{PetNotFoundError, PetValidationAlgebra}

import scala.language.higherKinds

/**
  *
  * @param repository where we get our data
  * @param validation something that provides validations to the service
  * @tparam F - this is the container for the things we work with, could be scala.concurrent.Future, Option, anything
  *           as long as it is a Monad
  */
class PetService[F[_] : Monad](implicit repository: PetRepositoryAlgebra[F], validation: PetValidationAlgebra[F]) {
  import fs2.util.syntax._

  def create(pet: Pet): F[Pet] = {
    for {
      _ <- validation.doesNotExist(pet)
      saved <- repository.put(pet)
    } yield saved
  }

  def get(id: Long)(implicit C: Catchable[F], M: Monad[F]): F[Pet] = {
    repository.get(id).flatMap {
      case None => C.fail(PetNotFoundError(id))
      case Some(found) => M.pure(found)
    }
  }

  /* In some circumstances we may care if we actually delete the pet; here we are idempotent and do not care */
  def delete(id: Long): F[Unit] = repository.delete(id).map(_ => ())

  def list(pageSize: Int, offset: Int): F[Seq[Pet]] = repository.list(pageSize, offset)
}
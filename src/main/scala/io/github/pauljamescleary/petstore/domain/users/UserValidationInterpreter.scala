package io.github.pauljamescleary.petstore.domain
package users

import cats._
import cats.data.EitherT
import cats.implicits._
import Function._

class UserValidationInterpreter[F[_]: Monad](userRepo: UserRepositoryAlgebra[F]) extends UserValidationAlgebra[F] {
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit] =
    EitherT {
      userRepo.findByUserName(user.userName).value.map {
        case None => Right(())
        case Some(_) => Left(UserAlreadyExistsError(user))
      }
    }

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] =
    EitherT {
      userId.map { id =>
        userRepo
          .get(id)
          .fold(UserNotFoundError.asLeft[Unit]) {
            const(Right(()))
          }
      }.getOrElse(
        Either.left[UserNotFoundError.type, Unit](UserNotFoundError).pure[F]
      )
    }
}

object UserValidationInterpreter {
  def apply[F[_]: Monad](repo: UserRepositoryAlgebra[F]): UserValidationAlgebra[F] =
    new UserValidationInterpreter[F](repo)
}

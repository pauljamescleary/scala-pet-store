package io.github.pauljamescleary.petstore.domain.users

import cats._
import cats.data.EitherT
import cats.implicits._
import io.github.pauljamescleary.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserValidationInterpreter[F[_]: Monad](userRepo: UserRepositoryAlgebra[F]) extends UserValidationAlgebra[F] {
  def doesNotExist(user: User) = EitherT {
    userRepo.findByUserName(user.userName).map {
      case None => Right(())
      case Some(_) => Left(UserAlreadyExistsError(user))
    }
  }

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] =
    EitherT {
      userId.map { id =>
        userRepo.get(id).map {
          case Some(_) => Right(())
          case _ => Left(UserNotFoundError)
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

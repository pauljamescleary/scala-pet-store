package io.github.pauljamescleary.petstore.domain.users

import cats._
import cats.data.EitherT
import cats.implicits._
import io.github.pauljamescleary.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserValidationInterpreter[F[_]: Monad](userRepo: UserRepositoryAlgebra[F]) extends UserValidationAlgebra[F] {
  def doesNotExist(user: User) = EitherT {
    userRepo.findByUserName(user.userName).value.map {
      case None => Right(())
      case Some(_) => Left(UserAlreadyExistsError(user))
    }
  }

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] =
    EitherT {
      userId match {
        case Some(id) =>
          userRepo.get(id).value.map {
            case Some(_) => Right(())
            case _ => Left(UserNotFoundError)
          }
        case _ =>
          Either.left[UserNotFoundError.type, Unit](UserNotFoundError).pure[F]
      }
    }
}

object UserValidationInterpreter {
  def apply[F[_]: Monad](repo: UserRepositoryAlgebra[F]): UserValidationAlgebra[F] =
    new UserValidationInterpreter[F](repo)
}

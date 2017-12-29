package io.github.pauljamescleary.petstore.domain.users

import cats._
import cats.data.EitherT
import cats.implicits._
import io.github.pauljamescleary.petstore.domain.UserAlreadyExistsError

class UserValidationInterpreter[F[_]: Monad](userRepo: UserRepositoryAlgebra[F]) extends UserValidationAlgebra[F] {
  def doesNotExist(user: User) = EitherT {
    userRepo.findByUserName(user.userName).map {
      case None => Right(())
      case Some(_) => Left(UserAlreadyExistsError(user))
    }
  }
}

object UserValidationInterpreter {
  def apply[F[_]: Monad](repo: UserRepositoryAlgebra[F]): UserValidationAlgebra[F] =
    new UserValidationInterpreter[F](repo)
}

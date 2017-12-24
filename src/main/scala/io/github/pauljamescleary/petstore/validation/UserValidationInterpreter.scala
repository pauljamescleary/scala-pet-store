package io.github.pauljamescleary.petstore.validation

import cats._
import cats.implicits._
import cats.data.EitherT
import io.github.pauljamescleary.petstore.model.User
import io.github.pauljamescleary.petstore.repository.UserRepositoryAlgebra

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

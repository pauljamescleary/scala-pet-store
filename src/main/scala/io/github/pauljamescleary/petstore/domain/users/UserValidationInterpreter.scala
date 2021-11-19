package io.github.pauljamescleary.petstore.domain
package users

import cats.Applicative
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all._

class UserValidationInterpreter(userRepo: UserRepositoryAlgebra)
    extends UserValidationAlgebra {
  def doesNotExist(user: User): IO[Either[UserAlreadyExistsError, Unit]] =
    OptionT(userRepo.findByUserName(user.userName))
      .map(UserAlreadyExistsError)
      .toLeft(())
      .value

  def exists(userId: Option[Long]): IO[Either[UserNotFoundError.type, Unit]] =
    userId match {
      case Some(id) =>
        userRepo.get(id)
          .toRight(UserNotFoundError)
          .void
          .value
      case None =>
        Left(UserNotFoundError).pure[IO]
    }
}

object UserValidationInterpreter {
  def apply(repo: UserRepositoryAlgebra): UserValidationAlgebra =
    new UserValidationInterpreter(repo)
}

package io.github.pauljamescleary.petstore.domain
package users

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._

class UserValidationInterpreter[F[_]: Applicative](userRepo: UserRepositoryAlgebra[F])
    extends UserValidationAlgebra[F] {
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit] =
    userRepo
      .findByUserName(user.userName)
      .map(UserAlreadyExistsError)
      .toLeft(())

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] =
    userId match {
      case Some(id) =>
        userRepo
          .get(id)
          .toRight(UserNotFoundError)
          .void
      case None =>
        EitherT.left[Unit](UserNotFoundError.pure[F])
    }
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](repo: UserRepositoryAlgebra[F]): UserValidationAlgebra[F] =
    new UserValidationInterpreter[F](repo)
}

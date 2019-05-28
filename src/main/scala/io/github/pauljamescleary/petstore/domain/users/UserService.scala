package io.github.pauljamescleary.petstore.domain
package users

import cats.data._
import cats.effect.Bracket
import cats.syntax.functor._

class UserService[F[_]: Bracket[?[_], Throwable]](
  userRepo: UserRepositoryAlgebra[F],
  validation: UserValidationAlgebra[F]
) {
  def createUser(user: User): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(userRepo.create(user))
    } yield saved

  def getUser(userId: Long): EitherT[F, UserNotFoundError.type, User] =
    userRepo.get(userId).toRight(UserNotFoundError)

  def getUserByName(userName: String): EitherT[F, UserNotFoundError.type, User] =
    userRepo.findByUserName(userName).toRight(UserNotFoundError)

  def deleteUser(userId: Long): F[Unit] =
    userRepo.delete(userId)
      .value
      .void

  def deleteByUserName(userName: String): F[Unit] =
    userRepo.deleteByUserName(userName)
      .value
      .void

  def update(user: User): EitherT[F, UserNotFoundError.type, User] =
    for {
      _ <- validation.exists(user.id)
      saved <- userRepo.update(user).toRight(UserNotFoundError)
    } yield saved

  def list(pageSize: Int, offset: Int): F[List[User]] =
    userRepo.list(pageSize, offset)
}

object UserService {
  def apply[F[_]: Bracket[?[_], Throwable]](
    repository: UserRepositoryAlgebra[F],
    validation: UserValidationAlgebra[F]
  ): UserService[F] =
    new UserService[F](repository, validation)
}

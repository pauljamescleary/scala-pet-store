package io.github.pauljamescleary.petstore.domain
package users

import cats.data._
import cats.effect.IO
import io.github.pauljamescleary.petstore.config.DatabaseConfig

class UserService(userRepo: UserRepositoryAlgebra, validation: UserValidationAlgebra) {
  def createUser(user: User): IO[Either[UserAlreadyExistsError, User]] =
    (for {
      _ <- EitherT(validation.doesNotExist(user))
      saved <- EitherT.liftF(userRepo.create(user))
    } yield saved).value

  def getUser(userId: Long): IO[Either[UserNotFoundError.type, User]]=
    userRepo.get(userId).toRight(UserNotFoundError).value

  def getUserByName(
      userName: String,
  ): IO[Either[UserNotFoundError.type, User]] =
    OptionT(userRepo.findByUserName(userName)).toRight(UserNotFoundError).value

  def deleteUser(userId: Long): IO[Unit] =
    userRepo.delete(userId).void

  def deleteByUserName(userName: String): IO[Unit] =
    userRepo.deleteByUserName(userName).void

  def update(user: User): IO[Either[UserNotFoundError.type, User]] =
    (for {
      _ <- EitherT(validation.exists(user.id))
      saved <- OptionT(userRepo.update(user)).toRight(UserNotFoundError)
    } yield saved).value

  def list(pageSize: Int, offset: Int): IO[List[User]] =
    userRepo.list(pageSize, offset)
}

object UserService {
  def apply(
      repository: UserRepositoryAlgebra,
      validation: UserValidationAlgebra,
  ): UserService =
    new UserService(repository, validation)
}

package io.github.pauljamescleary.petstore.domain.users

import cats._
import cats.data._
import io.github.pauljamescleary.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserService[F[_]: Monad](userRepo: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]) {

  def createUser(user: User): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(userRepo.put(user))
    } yield saved

  def getUser(userId: Long): EitherT[F, UserNotFoundError.type, User] =
    EitherT.fromOptionF(userRepo.get(userId).value, UserNotFoundError)

  def getUserByName(userName: String): EitherT[F, UserNotFoundError.type, User] =
    EitherT.fromOptionF(userRepo.findByUserName(userName).value, UserNotFoundError)

  def deleteUser(userId: Long): F[Unit] = userRepo.delete(userId)

  def deleteByUserName(userName: String): F[Unit] =
    userRepo.deleteByUserName(userName)

  def update(user: User): EitherT[F, UserNotFoundError.type, User] =
    for {
      _ <- validation.exists(user.id)
      saved <- EitherT.liftF(userRepo.update(user))
    } yield saved

  def list(pageSize: Int, offset: Int): F[List[User]] =
    userRepo.list(pageSize, offset)
}

object UserService {
  def apply[F[_]: Monad](repository: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]): UserService[F] =
    new UserService[F](repository, validation)
}

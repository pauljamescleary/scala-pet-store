package io.github.pauljamescleary.petstore.domain.users

import cats._
import cats.data._
import cats.effect.Bracket
import cats.syntax.functor._
import io.github.pauljamescleary.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserService[F[_]: Monad](userRepo: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]) {
  type B = Bracket[F, Throwable]

  def createUser(user: User)(implicit b: B): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(userRepo.create(user))
    } yield saved

  def getUser(userId: Long)(implicit b: B): EitherT[F, UserNotFoundError.type, User] =
    EitherT.fromOptionF(userRepo.get(userId), UserNotFoundError)

  def getUserByName(userName: String)(implicit b: B): EitherT[F, UserNotFoundError.type, User] =
    EitherT.fromOptionF(userRepo.findByUserName(userName), UserNotFoundError)

  def deleteUser(userId: Long)(implicit b: B): F[Unit] = userRepo.delete(userId).as(())

  def deleteByUserName(userName: String)(implicit b: B): F[Unit] =
    userRepo.deleteByUserName(userName).as(())

  def update(user: User)(implicit b: B): EitherT[F, UserNotFoundError.type, User] =
    for {
      _ <- validation.exists(user.id)
      saved <- EitherT.fromOptionF(userRepo.update(user), UserNotFoundError)
    } yield saved

  def list(pageSize: Int, offset: Int)(implicit b: B): F[List[User]] =
    userRepo.list(pageSize, offset)
}

object UserService {
  def apply[F[_]: Monad](repository: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]): UserService[F] =
    new UserService[F](repository, validation)
}

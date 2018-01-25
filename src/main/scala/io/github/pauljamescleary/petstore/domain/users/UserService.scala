package io.github.pauljamescleary.petstore.domain.users

import scala.language.higherKinds

import cats._
import cats.data._
import io.github.pauljamescleary.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserService[F[_]](userRepo: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]) {
  import cats.syntax.all._

  def createUser(user: User)(implicit M: Monad[F]): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(userRepo.create(user))
    } yield saved

  def getUser(userId: Long)(implicit M: Monad[F]): EitherT[F, UserNotFoundError.type, User] =
    EitherT {
      userRepo.get(userId).map {
        case None => Left(UserNotFoundError)
        case Some(user) => Right(user)
      }
    }

  def getUserByName(userName: String)(implicit M: Monad[F]): EitherT[F, UserNotFoundError.type, User] =
    EitherT {
      userRepo.findByUserName(userName).map {
        case None => Left(UserNotFoundError)
        case Some(user) => Right(user)
      }
    }

  def deleteUser(userId: Long): F[Option[User]] = userRepo.delete(userId)

  def deleteByName(userName: String)(implicit M: Monad[F]): F[Unit] =
    userRepo.deleteByUserName(userName).as(())

  def update(user: User)(implicit M: Monad[F]): EitherT[F, UserNotFoundError.type, User] =
    for {
      _ <- validation.exists(user.id)
      saved <- EitherT.fromOptionF(userRepo.update(user), UserNotFoundError)
    } yield saved

  def list(pageSize: Int, offset: Int): F[List[User]] =
    userRepo.list(pageSize, offset)
}

object UserService {
  def apply[F[_]](repository: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]): UserService[F] =
    new UserService[F](repository, validation)
}

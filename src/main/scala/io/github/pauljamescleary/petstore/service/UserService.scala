package io.github.pauljamescleary.petstore.service

import cats.Monad
import cats.data.EitherT
import io.github.pauljamescleary.petstore.model.User
import io.github.pauljamescleary.petstore.repository.UserRepositoryAlgebra
import io.github.pauljamescleary.petstore.validation.UserNotFoundError

import scala.language.higherKinds

class UserService[F[_]](userRepo: UserRepositoryAlgebra[F]) {
  import cats.syntax.all._

  def createUser(user: User): F[User] = userRepo.put(user)

  def getUser(userId: Long)(implicit M: Monad[F]): EitherT[F, UserNotFoundError.type, User] =
    EitherT {
      userRepo.get(userId).map {
        case None => Left(UserNotFoundError)
        case Some(user) => Right(user)
      }
    }

  def deleteUser(userId: Long): F[Option[User]] = userRepo.delete(userId)
}

object UserService {
  def apply[F[_]](repository: UserRepositoryAlgebra[F]): UserService[F] =
    new UserService[F](repository)
}

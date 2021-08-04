package io.github.pauljamescleary.petstore.domain
package users

import cats.data._
import cats.implicits._
import cats.Monad
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class UserService[F[_]: Logger: Monad](
    userRepo: UserRepositoryAlgebra[F],
    validation: UserValidationAlgebra[F],
) {
  def createUser(user: User): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _ <- EitherT.liftF(Logger[F].info(s"Creating user $user"))
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(userRepo.create(user))
    } yield saved

  def getUser(userId: Long): EitherT[F, UserNotFoundError.type, User] =
    EitherT
      .liftF[F, UserNotFoundError.type, Unit](Logger[F].info(s"Fetching user by id $userId"))
      .flatMap(_ => userRepo.get(userId).toRight(UserNotFoundError))

  def getUserByName(
      userName: String,
  ): EitherT[F, UserNotFoundError.type, User] = EitherT
    .liftF(Logger[F].info(s"Looking up user by name $userName"))
    .flatMap(_ => userRepo.findByUserName(userName).toRight(UserNotFoundError))

  def deleteUser(userId: Long): F[Unit] =
    Logger[F].info(s"Deleting user with id $userId") >> userRepo.delete(userId).value.void

  def deleteByUserName(userName: String): F[Unit] =
    Logger[F].info(s"Deleting user with name $userName") >>
      userRepo.deleteByUserName(userName).value.void

  def update(user: User): EitherT[F, UserNotFoundError.type, User] =
    for {
      _ <- EitherT.liftF(Logger[F].info(s"Updating user $user"))
      _ <- validation.exists(user.id)
      saved <- userRepo.update(user).toRight(UserNotFoundError)
    } yield saved

  def list(pageSize: Int, offset: Int): F[List[User]] =
    Logger[F].info(s"Requeisting $pageSize users with offset $offset") >>
      userRepo.list(pageSize, offset)
}

object UserService {
  def apply[F[_]: Sync](
      repository: UserRepositoryAlgebra[F],
      validation: UserValidationAlgebra[F],
  ): Resource[F, UserService[F]] =
    Resource.eval(Slf4jLogger.create[F]).map { implicit logger: Logger[F] =>
      new UserService[F](repository, validation)
    }
}

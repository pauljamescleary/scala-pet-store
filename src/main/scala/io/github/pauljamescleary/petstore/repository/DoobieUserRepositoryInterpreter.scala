package io.github.pauljamescleary.petstore.repository

import doobie._
import doobie.implicits._
import cats._
import cats.implicits._
import io.github.pauljamescleary.petstore.model.User

import scala.language.higherKinds

class DoobieUserRepositoryInterpreter[F[_]: Monad](val xa: Transactor[F])
  extends UserRepositoryAlgebra[F] {

  def put(user: User): F[User] = {
    val insert: ConnectionIO[User] =
      for {
        id <- sql"""
            REPLACE INTO USERS (USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, PHONE)
              VALUES (${user.userName}, ${user.firstName}, ${user.lastName}, ${user.email}, ${user.password}, ${user.phone})
          """.update.withUniqueGeneratedKeys[Long]("ID")
      } yield user.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(userId: Long): F[Option[User]] =
    sql"""
      SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, PHONE, ID
        FROM USERS
       WHERE ID = $userId
     """.query[User].option.transact(xa)

  def delete(userId: Long): F[Option[User]] =
    get(userId).flatMap {
      case Some(user) =>
        sql"DELETE FROM USERS WHERE ID = $userId".update.run
          .transact(xa)
          .map(_ => Some(user))
      case None =>
        none[User].pure[F]
    }
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter(xa)
}


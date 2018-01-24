package io.github.pauljamescleary.petstore.infrastructure.repository.doobie

import scala.language.higherKinds

import cats._
import cats.data.OptionT
import cats.implicits._
import doobie._
import doobie.implicits._
import io.github.pauljamescleary.petstore.domain.users.{User, UserRepositoryAlgebra}
import pagination._

private object UserQueries {
  def insert(user: User): Update0 = sql"""
    REPLACE INTO USERS (USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, PHONE)
    VALUES (${user.userName}, ${user.firstName}, ${user.lastName}, ${user.email}, ${user.password}, ${user.phone})
  """.update

  def select(userId: Long): Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, PHONE, ID
    FROM USERS
    WHERE ID = $userId
  """.query

  def byUserName(userName: String): Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, PHONE, ID
    FROM USERS
    WHERE USER_NAME = $userName
  """.query[User]

  def delete(userId: Long): Update0 = sql"""
    DELETE FROM USERS WHERE ID = $userId
  """.update

  val selectAll: Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, PHONE, ID
    FROM USERS
  """.query
}

class DoobieUserRepositoryInterpreter[F[_]: Monad](val xa: Transactor[F])
  extends UserRepositoryAlgebra[F] {

  import UserQueries._

  def put(user: User): F[User] = {
    val insert: ConnectionIO[User] =
      for {
        id <- UserQueries.insert(user).withUniqueGeneratedKeys[Long]("ID")
      } yield user.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(userId: Long): F[Option[User]] = select(userId).option.transact(xa)

  def findByUserName(userName: String): F[Option[User]] =
    byUserName(userName).option.transact(xa)

  def delete(userId: Long): F[Option[User]] =
    get(userId).flatMap {
      case Some(user) => UserQueries.delete(userId).run.transact(xa).map(_ => Some(user))
      case None => none[User].pure[F]
    }

  def deleteByUserName(userName: String): F[Option[User]] =
    OptionT(findByUserName(userName)).mapFilter(_.id).flatMapF(delete).value

  def list(pageSize: Int, offset: Int): F[List[User]] =
    paginate(pageSize, offset)(selectAll).list.transact(xa)
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter(xa)
}


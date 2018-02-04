package io.github.pauljamescleary.petstore.infrastructure.repository.doobie

import cats._
import cats.data.OptionT
import cats.implicits._
import doobie._
import doobie.implicits._
import io.github.pauljamescleary.petstore.domain.users.{User, UserRepositoryAlgebra}
import SQLPagination._

private object UserSQL {
  def insert(user: User): Update0 = sql"""
    INSERT INTO USERS (USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE)
    VALUES (${user.userName}, ${user.firstName}, ${user.lastName}, ${user.email}, ${user.hash}, ${user.phone})
  """.update

  def update(user: User, id: Long): Update0 = sql"""
    UPDATE USERS
    SET FIRST_NAME = ${user.firstName}, LAST_NAME = ${user.lastName}, EMAIL = ${user.email}, HASH = ${user.hash}, PHONE = ${user.phone}
    WHERE ID = $id
  """.update

  def select(userId: Long): Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID
    FROM USERS
    WHERE ID = $userId
  """.query

  def byUserName(userName: String): Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID
    FROM USERS
    WHERE USER_NAME = $userName
  """.query[User]

  def delete(userId: Long): Update0 = sql"""
    DELETE FROM USERS WHERE ID = $userId
  """.update

  val selectAll: Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID
    FROM USERS
  """.query
}

class DoobieUserRepositoryInterpreter[F[_]: Monad](val xa: Transactor[F])
  extends UserRepositoryAlgebra[F] {

  import UserSQL._

  def put(user: User): F[User] =
    insert(user).withUniqueGeneratedKeys[Long]("ID").map(id => user.copy(id = id.some)).transact(xa)

  def updateSafe(user: User): OptionT[F, User] = OptionT.fromOption[F](user.id).flatMapF { id =>
    UserSQL.update(user, id).run.as(user.some).transact(xa)
  }

  def update(user: User): F[User] = OptionT.fromOption[F](user.id).semiflatMap { id =>
    UserSQL.update(user, id).run.transact(xa)
  }.value.as(user)

  def get(userId: Long): OptionT[F, User] = OptionT(select(userId).option.transact(xa))

  def findByUserName(userName: String): OptionT[F, User] =
    OptionT(byUserName(userName).option.transact(xa))

  def delete(userId: Long): F[Unit] = UserSQL.delete(userId).run.transact(xa).as(())

  def deleteByUserName(userName: String): F[Unit] =
    findByUserName(userName).mapFilter(_.id).semiflatMap(delete).value.as(())

  def list(pageSize: Int, offset: Int): F[List[User]] =
    paginate(pageSize, offset)(selectAll).list.transact(xa)
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter(xa)
}


package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import cats.data.OptionT
import cats.effect.{Bracket, IO}
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.circe.parser.decode
import io.circe.syntax._
import domain.users.{Role, User, UserRepositoryAlgebra}
import io.github.pauljamescleary.petstore.infrastructure.repository.doobie.SQLPagination._
import tsec.authentication.IdentityStore

private object UserSQL {
  // H2 does not support JSON data type.
  implicit val roleMeta: Meta[Role] =
    Meta[String].imap(decode[Role](_).leftMap(throw _).merge)(_.asJson.toString)

  def insert(user: User): Update0 = sql"""
    INSERT INTO USERS (USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ROLE)
    VALUES (${user.userName}, ${user.firstName}, ${user.lastName}, ${user.email}, ${user.hash}, ${user.phone}, ${user.role})
  """.update

  def update(user: User, id: Long): Update0 = sql"""
    UPDATE USERS
    SET FIRST_NAME = ${user.firstName}, LAST_NAME = ${user.lastName},
        EMAIL = ${user.email}, HASH = ${user.hash}, PHONE = ${user.phone}, ROLE = ${user.role}
    WHERE ID = $id
  """.update

  def select(userId: Long): Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID, ROLE
    FROM USERS
    WHERE ID = $userId
  """.query

  def byUserName(userName: String): Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID, ROLE
    FROM USERS
    WHERE USER_NAME = $userName
  """.query[User]

  def delete(userId: Long): Update0 = sql"""
    DELETE FROM USERS WHERE ID = $userId
  """.update

  val selectAll: Query0[User] = sql"""
    SELECT USER_NAME, FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID, ROLE
    FROM USERS
  """.query
}

class DoobieUserRepositoryInterpreter(val xa: Transactor[IO])
    extends UserRepositoryAlgebra with IdentityStore[IO, Long, User] { self =>
  import UserSQL._

  def create(user: User): IO[User] =
    insert(user).withUniqueGeneratedKeys[Long]("ID").map(id => user.copy(id = id.some)).transact(xa)

  def update(user: User): IO[Option[User]] =
    OptionT.fromOption[IO](user.id).semiflatMap { id =>
      UserSQL.update(user, id).run.transact(xa).as(user)
    }.value

  def get(userId: Long): OptionT[IO, User] = OptionT(select(userId).option.transact(xa))

  def findByUserName(userName: String): IO[Option[User]] =
    OptionT(byUserName(userName).option.transact(xa)).value

  def delete(userId: Long): IO[Option[User]] =
    get(userId).semiflatMap(user => UserSQL.delete(userId).run.transact(xa).as(user)).value

  def deleteByUserName(userName: String): IO[Option[User]] =
    findByUserName(userName).flatMap {
      case Some(value) =>
        value.id match {
          case Some(value) => delete(value)
          case None => IO.none
        }
      case None => IO.none
    }

  def list(pageSize: Int, offset: Int): IO[List[User]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)
}

object DoobieUserRepositoryInterpreter {
  def apply(xa: Transactor[IO]): DoobieUserRepositoryInterpreter =
    new DoobieUserRepositoryInterpreter(xa)
}

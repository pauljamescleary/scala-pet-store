package io.github.pauljamescleary.petstore
package infrastructure.repository.inmemory

import java.util.Random

import cats.implicits._
import cats.Applicative
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.data.OptionT
import domain.users.{User, UserRepositoryAlgebra}
import tsec.authentication.IdentityStore

import scala.collection.concurrent.TrieMap

class UserRepositoryInMemoryInterpreter[F[_]: Applicative: Logger]
    extends UserRepositoryAlgebra[F]
    with IdentityStore[F, Long, User] {
  private val cache = new TrieMap[Long, User]

  private val random = new Random

  def create(user: User): F[User] = {
    val id = random.nextLong()
    val toSave = user.copy(id = id.some)
    cache += (id -> toSave)
    Logger[F].debug(s"Put user $user into Cache")
    toSave.pure[F]
  }

  def update(user: User): OptionT[F, User] =
    OptionT {
      Logger[F].debug(s"Updating user $user") *>
        user.id.traverse { id =>
          cache.update(id, user)
          user.pure[F]
        }
    }

  def get(id: Long): OptionT[F, User] = OptionT(
    Logger[F].debug(s"Fetching user with id $id") *> cache.get(id).pure[F],
  )

  def delete(id: Long): OptionT[F, User] = OptionT(
    Logger[F].debug(s"Deleting user with id $id") *> cache.remove(id).pure[F],
  )

  def findByUserName(userName: String): OptionT[F, User] =
    OptionT(
      Logger[F].debug(s"Looking up user with name $userName") *>
        cache.values.find(u => u.userName == userName).pure[F],
    )

  def list(pageSize: Int, offset: Int): F[List[User]] =
    Logger[F].debug(s"Listing $pageSize users with offset $offset") *>
      cache.values.toList.sortBy(_.lastName).slice(offset, offset + pageSize).pure[F]

  def deleteByUserName(userName: String): OptionT[F, User] =
    OptionT(
      Logger[F].debug(s"Deleting user with name $userName") *>
        (for {
          user <- cache.values.find(u => u.userName == userName)
          removed <- cache.remove(user.id.get)
        } yield removed).pure[F],
    )
}

object UserRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative: Sync]() =
    Resource.eval(Slf4jLogger.create[F]).map { implicit logger: Logger[F] =>
      new UserRepositoryInMemoryInterpreter[F]
    }
}

package io.github.pauljamescleary.petstore
package infrastructure.repository.inmemory

import java.util.Random

import cats.implicits._
import cats.Applicative
import cats.data.OptionT
import domain.users.{User, UserRepositoryAlgebra}
import tsec.authentication.IdentityStore

import scala.collection.concurrent.TrieMap

class UserRepositoryInMemoryInterpreter[F[_]: Applicative]
    extends UserRepositoryAlgebra[F]
    with IdentityStore[F, Long, User] {
  private val cache = new TrieMap[Long, User]

  private val random = new Random

  def create(user: User): F[User] = {
    val id = random.nextLong()
    val toSave = user.copy(id = id.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  def update(user: User): OptionT[F, User] = OptionT {
    user.id.traverse { id =>
      cache.update(id, user)
      user.pure[F]
    }
  }

  def get(id: Long): OptionT[F, User] =
    OptionT.fromOption(cache.get(id))

  def delete(id: Long): OptionT[F, User] =
    OptionT.fromOption(cache.remove(id))

  def findByUserName(userName: String): OptionT[F, User] =
    OptionT.fromOption(cache.values.find(u => u.userName == userName))

  def list(pageSize: Int, offset: Int): F[List[User]] =
    cache.values.toList.sortBy(_.lastName).slice(offset, offset + pageSize).pure[F]

  def deleteByUserName(userName: String): OptionT[F, User] =
    OptionT.fromOption(
      for {
        user <- cache.values.find(u => u.userName == userName)
        removed <- cache.remove(user.id.get)
      } yield removed,
    )
}

object UserRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() =
    new UserRepositoryInMemoryInterpreter[F]
}

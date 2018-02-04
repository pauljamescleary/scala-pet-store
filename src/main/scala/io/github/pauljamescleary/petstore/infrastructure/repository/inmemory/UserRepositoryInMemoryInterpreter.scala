package io.github.pauljamescleary.petstore.infrastructure.repository.inmemory

import java.util.Random

import cats.implicits._
import cats.Applicative
import cats.data.OptionT
import io.github.pauljamescleary.petstore.domain.users.{User, UserRepositoryAlgebra}

import scala.collection.concurrent.TrieMap

class UserRepositoryInMemoryInterpreter[F[_]: Applicative] extends UserRepositoryAlgebra[F] {

  private val cache = new TrieMap[Long, User]

  private val random = new Random

  def put(user: User): F[User] = {
    val id = random.nextLong
    val toSave = user.copy(id = id.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  def update(user: User): F[User] = user.id.traverse{ id =>
    cache.update(id, user)
    user.pure[F]
  }.as(user)

  def get(id: Long): OptionT[F, User] = OptionT.fromOption[F](cache.get(id))

  def delete(id: Long): F[Unit] = cache.remove(id).pure[F].as(())

  def findByUserName(userName: String): OptionT[F, User] =
    OptionT.fromOption[F](cache.values.find(u => u.firstName == userName))

  def list(pageSize: Int, offset: Int): F[List[User]] =
    cache.values.toList.sortBy(_.lastName).slice(offset, offset + pageSize).pure[F]

  def deleteByUserName(userName: String): F[Unit] = {
    val deleted: Option[User] = for {
      user <- cache.values.find(u => u.userName == userName)
      removed <- cache.remove(user.id.get)
    } yield removed

    deleted.pure[F].as(())
  }
}

object UserRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new UserRepositoryInMemoryInterpreter[F]
}

package io.github.pauljamescleary.petstore.infrastructure.repository.inmemory

import java.util.Random

import cats._
import cats.implicits._
import cats.Applicative
import io.github.pauljamescleary.petstore.domain.users.{User, UserRepositoryAlgebra}

import scala.collection.concurrent.TrieMap

class UserRepositoryInMemoryInterpreter[F[_]: Applicative] extends UserRepositoryAlgebra[F] {

  private val cache = new TrieMap[Long, User]

  private val random = new Random

  override def put(user: User): F[User] = {
    val toSave =
      if (user.id.isDefined) user else user.copy(id = Some(random.nextLong))
    toSave.id.foreach { cache.put(_, toSave) }
    toSave.pure[F]
  }

  override def get(id: Long): F[Option[User]] = cache.get(id).pure[F]

  override def delete(id: Long): F[Option[User]] = cache.remove(id).pure[F]

  override def findByUserName(userName: String): F[Option[User]] =
    cache.values.find(u => u.firstName == userName).pure[F]

  override def list(pageSize: Int, offset: Int): F[List[User]] =
    cache.values.toList.sortBy(_.lastName).slice(offset, offset + pageSize).pure[F]

  override def deleteByUserName(userName: String): F[Option[User]] = {
    val deleted = for {
      user <- cache.values.find(u => u.userName == userName)
      removed <- cache.remove(user.id.get)
    } yield removed
    deleted.pure[F]
  }
}

object UserRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new UserRepositoryInMemoryInterpreter[F]
}

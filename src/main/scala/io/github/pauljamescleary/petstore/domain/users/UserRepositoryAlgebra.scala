package io.github.pauljamescleary.petstore.domain.users

import scala.language.higherKinds

trait UserRepositoryAlgebra[F[_]] {
  def create(user: User): F[User]

  def update(user: User): F[Option[User]]

  def get(userId: Long): F[Option[User]]

  def delete(userId: Long): F[Option[User]]

  def findByUserName(userName: String): F[Option[User]]

  def deleteByUserName(userName: String): F[Option[User]]

  def list(pageSize: Int, offset: Int): F[List[User]]
}

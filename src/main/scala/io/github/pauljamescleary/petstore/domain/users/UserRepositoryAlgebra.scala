package io.github.pauljamescleary.petstore.domain.users

import cats.effect.Bracket

trait UserRepositoryAlgebra[F[_]] {
  type B = Bracket[F, Throwable]

  def create(user: User)(implicit b: B): F[User]

  def update(user: User)(implicit b: B): F[Option[User]]

  def get(userId: Long)(implicit b: B): F[Option[User]]

  def delete(userId: Long)(implicit b: B): F[Option[User]]

  def findByUserName(userName: String)(implicit b: B): F[Option[User]]

  def deleteByUserName(userName: String)(implicit b: B): F[Option[User]]

  def list(pageSize: Int, offset: Int)(implicit b: B): F[List[User]]
}

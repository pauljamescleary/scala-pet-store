package io.github.pauljamescleary.petstore.domain.users

import cats.data.OptionT
import cats.effect.IO

trait UserRepositoryAlgebra {
  def create(user: User): IO[User]

  def update(user: User): IO[Option[User]]

  def get(userId: Long): OptionT[IO, User]

  def delete(userId: Long): IO[Option[User]]

  def findByUserName(userName: String): IO[Option[User]]

  def deleteByUserName(userName: String): IO[Option[User]]

  def list(pageSize: Int, offset: Int): IO[List[User]]
}

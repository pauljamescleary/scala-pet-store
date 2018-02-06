package io.github.pauljamescleary.petstore.domain.users

import cats.data.OptionT

trait UserRepositoryAlgebra[F[_]]{

  def put(elem: User): F[User]

  def get(id: Long): OptionT[F, User]

  def update(v: User): F[User]

  def delete(id: Long): F[Unit]

  def findByUserName(userName: String): OptionT[F, User]

  def deleteByUserName(userName: String): F[Unit]

  def list(pageSize: Int, offset: Int): F[List[User]]
}

package io.github.pauljamescleary.petstore.repository

import io.github.pauljamescleary.petstore.model.User

import scala.language.higherKinds

trait UserRepositoryAlgebra[F[_]] {

  def put(user: User): F[User]

  def get(userId: Long): F[Option[User]]

  def delete(userId: Long): F[Option[User]]
}

package io.github.pauljamescleary.petstore.domain.repository

import io.github.pauljamescleary.petstore.domain.model.User

import scala.language.higherKinds

trait UserRepositoryAlgebra[F[_]] {

  def put(user: User): F[User]

  def get(userId: Long): F[Option[User]]

  def delete(userId: Long): F[Option[User]]

  def findByUserName(userName: String): F[Option[User]]
}

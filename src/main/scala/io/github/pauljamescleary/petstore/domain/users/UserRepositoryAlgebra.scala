package io.github.pauljamescleary.petstore.domain.users

import cats.data.OptionT
import tsec.authentication.BackingStore

trait UserBackingStore[F[_]] extends BackingStore[F, Long, User]

trait UserRepositoryAlgebra[F[_]] extends UserBackingStore[F]{
  def findByUserName(userName: String): OptionT[F, User]

  def deleteByUserName(userName: String): F[Unit]

  def list(pageSize: Int, offset: Int): F[List[User]]
}

package io.github.pauljamescleary.petstore
package infrastructure.authentication

import cats.data.OptionT
import tsec.authentication.BackingStore
import domain.users.{User, UserRepositoryAlgebra}

abstract class UserBackingStore[F[_]] extends BackingStore[F, Long, User]

object UserBackingStore {

  /**
    * A transformation between the UserRepositoryAlgebra in our domain and a BackingStore
    * for users which is required by tsec at our endpoint.
    */
  def trans[F[_]](A: UserRepositoryAlgebra[F]): UserBackingStore[F] = new UserBackingStore[F] {
    def put(elem: User): F[User] = A.put(elem)
    def get(id: Long): OptionT[F, User] = A.get(id)
    def update(v: User): F[User] = A.update(v)
    def delete(id: Long): F[Unit] = A.delete(id)
  }
}


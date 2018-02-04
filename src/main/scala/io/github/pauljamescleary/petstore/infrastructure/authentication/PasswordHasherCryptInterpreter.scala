package io.github.pauljamescleary.petstore
package infrastructure.authentication

import cats.effect.Sync
import tsec.passwordhashers.PasswordHash
import domain.authentication.CryptAlgebra
import tsec.passwordhashers.core.PasswordHasher

class PasswordHasherCryptInterpreter[F[_] : Sync, A](
  implicit H: PasswordHasher[A]
) extends CryptAlgebra[F, A] {

  def hash(password: String): F[PasswordHash[A]] = H.hashpw[F](password)

  def check(password: String, hash: PasswordHash[A]): F[Boolean] = H.checkpw(password, hash)

}

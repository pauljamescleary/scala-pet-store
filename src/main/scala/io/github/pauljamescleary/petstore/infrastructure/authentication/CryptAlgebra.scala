package io.github.pauljamescleary.petstore.infrastructure.authentication

import tsec.passwordhashers.PasswordHash


trait CryptAlgebra[F[_], A]{
  def hash(password: String): F[PasswordHash[A]]

  def check(password: String, hash: PasswordHash[A]) : F[Boolean]
}

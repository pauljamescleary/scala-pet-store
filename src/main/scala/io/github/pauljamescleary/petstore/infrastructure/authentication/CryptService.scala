package io.github.pauljamescleary.petstore.infrastructure.authentication

import cats._
import tsec.passwordhashers.PasswordHash

class CryptService[F[_] : Monad, A](authRepo : CryptAlgebra[F, A]){
  def hash(password: String): F[PasswordHash[A]] = authRepo.hash(password)

  def check(password: String, hash: PasswordHash[A]) : F[Boolean] = authRepo.check(password, hash)
}

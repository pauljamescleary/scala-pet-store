package io.github.pauljamescleary.petstore.domain.users

import scala.language.higherKinds

import cats.data.EitherT
import io.github.pauljamescleary.petstore.domain.UserAlreadyExistsError

trait UserValidationAlgebra[F[_]] {

  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit]
}

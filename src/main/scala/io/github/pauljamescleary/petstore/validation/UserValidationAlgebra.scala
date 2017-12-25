package io.github.pauljamescleary.petstore.validation

import cats.data.EitherT
import io.github.pauljamescleary.petstore.model.User

import scala.language.higherKinds

trait UserValidationAlgebra[F[_]] {

  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit]
}

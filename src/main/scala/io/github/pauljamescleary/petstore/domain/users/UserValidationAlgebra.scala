package io.github.pauljamescleary.petstore.domain.users

import scala.language.higherKinds
import cats.data.EitherT
import cats.effect.Bracket
import io.github.pauljamescleary.petstore.domain.{UserAlreadyExistsError, UserNotFoundError}

trait UserValidationAlgebra[F[_]] {
  type B = Bracket[F, Throwable]

  def doesNotExist(user: User)(implicit b: B): EitherT[F, UserAlreadyExistsError, Unit]

  def exists(userId: Option[Long])(implicit b: B): EitherT[F, UserNotFoundError.type, Unit]
}

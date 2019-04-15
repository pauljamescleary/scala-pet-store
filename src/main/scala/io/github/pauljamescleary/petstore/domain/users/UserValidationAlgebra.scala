package io.github.pauljamescleary.petstore.domain
package users

import scala.language.higherKinds
import cats.data.EitherT

trait UserValidationAlgebra[F[_]] {

  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit]

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit]
}

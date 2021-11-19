package io.github.pauljamescleary.petstore.domain
package users

import cats.effect.IO

trait UserValidationAlgebra {
  def doesNotExist(user: User): IO[Either[UserAlreadyExistsError, Unit]]

  def exists(userId: Option[Long]): IO[Either[UserNotFoundError.type, Unit]]
}

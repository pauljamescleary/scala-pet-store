package io.github.pauljamescleary.petstore.domain.users

import cats.syntax.applicative._
import cats.effect.IO
import tsec.authorization.AuthorizationInfo

case class User(
    userName: String,
    firstName: String,
    lastName: String,
    email: String,
    hash: String,
    phone: String,
    id: Option[Long] = None,
    role: Role,
)

object User {
  implicit val authRole: AuthorizationInfo[IO, Role, User] =
    new AuthorizationInfo[IO, Role, User] {
      def fetchInfo(u: User): IO[Role] = u.role.pure[IO]
    }
}

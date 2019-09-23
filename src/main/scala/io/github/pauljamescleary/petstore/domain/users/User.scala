package io.github.pauljamescleary.petstore.domain.users

import cats.Applicative
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
  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, User] =
    new AuthorizationInfo[F, Role, User] {
      def fetchInfo(u: User): F[Role] = F.pure(u.role)
    }
}

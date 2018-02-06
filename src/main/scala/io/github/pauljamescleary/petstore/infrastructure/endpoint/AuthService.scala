package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import domain.users.User
import tsec.authentication.AuthenticatorService

object AuthService{
  type AuthService[F[_], K] = AuthenticatorService[F, Long, User, K]
}

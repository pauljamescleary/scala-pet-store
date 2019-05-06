package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import io.github.pauljamescleary.petstore.domain.users.User
import org.http4s.Response
import tsec.authentication.{AugmentedJWT, SecuredRequest, TSecAuthService}

package object alias {
  type AuthService[F[_], Auth] = TSecAuthService[User, AugmentedJWT[Auth, Long], F]
  type AuthEndpoint[F[_], Auth] = PartialFunction[SecuredRequest[F, User, AugmentedJWT[Auth, Long]], F[Response[F]]]
}

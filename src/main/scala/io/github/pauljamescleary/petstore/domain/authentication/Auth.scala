package io.github.pauljamescleary.petstore.domain.authentication

import cats.effect._
import io.github.pauljamescleary.petstore.domain.users.{Role, User}
import org.http4s.Response
import tsec.authentication.{
  AugmentedJWT,
  BackingStore,
  IdentityStore,
  JWTAuthenticator,
  SecuredRequest,
  TSecAuthService,
}
import tsec.authorization.BasicRBAC
import tsec.common.SecureRandomId
import tsec.jws.mac.JWSMacCV
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.mac.jca.MacSigningKey

import scala.concurrent.duration._

object Auth {
  def jwtAuthenticator[Auth: JWTMacAlgo](
      key: MacSigningKey[Auth],
      authRepo: BackingStore[IO, SecureRandomId, AugmentedJWT[Auth, Long]],
      userRepo: IdentityStore[IO, Long, User],
  )(implicit cv: JWSMacCV[IO, Auth]): JWTAuthenticator[IO, Long, User, Auth] =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = 1.hour,
      maxIdle = None,
      tokenStore = authRepo,
      identityStore = userRepo,
      signingKey = key,
    )

  private def _allRoles[Auth]: BasicRBAC[IO, Role, User, Auth] =
    BasicRBAC.all[IO, Role, User, Auth]

  def allRoles[Auth](
      pf: PartialFunction[SecuredRequest[IO, User, AugmentedJWT[Auth, Long]], IO[Response[IO]]],
  ): TSecAuthService[User, AugmentedJWT[Auth, Long], IO] =
    TSecAuthService.withAuthorization(_allRoles[AugmentedJWT[Auth, Long]])(pf)

  def allRolesHandler[Auth](
      pf: PartialFunction[SecuredRequest[IO, User, AugmentedJWT[Auth, Long]], IO[Response[IO]]],
  )(
      onNotAuthorized: TSecAuthService[User, AugmentedJWT[Auth, Long], IO],
  ): TSecAuthService[User, AugmentedJWT[Auth, Long], IO] =
    TSecAuthService.withAuthorizationHandler(_allRoles[AugmentedJWT[Auth, Long]])(
      pf,
      onNotAuthorized.run,
    )

  private def _adminOnly[Auth]: BasicRBAC[IO, Role, User, Auth] =
    BasicRBAC[IO, Role, User, Auth](Role.Admin)

  def adminOnly[Auth](
      pf: PartialFunction[SecuredRequest[IO, User, AugmentedJWT[Auth, Long]], IO[Response[IO]]],
  ): TSecAuthService[User, AugmentedJWT[Auth, Long], IO] =
    TSecAuthService.withAuthorization(_adminOnly[AugmentedJWT[Auth, Long]])(pf)
}

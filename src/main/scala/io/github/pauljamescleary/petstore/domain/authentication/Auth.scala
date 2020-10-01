package io.github.pauljamescleary.petstore.domain.authentication

import cats.MonadError
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
  def jwtAuthenticator[F[_]: Sync, Auth: JWTMacAlgo](
      key: MacSigningKey[Auth],
      authRepo: BackingStore[F, SecureRandomId, AugmentedJWT[Auth, Long]],
      userRepo: IdentityStore[F, Long, User],
  )(implicit cv: JWSMacCV[F, Auth]): JWTAuthenticator[F, Long, User, Auth] =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = 1.hour,
      maxIdle = None,
      tokenStore = authRepo,
      identityStore = userRepo,
      signingKey = key,
    )

  private def _allRoles[F[_], Auth](implicit
      F: MonadError[F, Throwable],
  ): BasicRBAC[F, Role, User, Auth] =
    BasicRBAC.all[F, Role, User, Auth]

  def allRoles[F[_], Auth](
      pf: PartialFunction[SecuredRequest[F, User, AugmentedJWT[Auth, Long]], F[Response[F]]],
  )(implicit F: MonadError[F, Throwable]): TSecAuthService[User, AugmentedJWT[Auth, Long], F] =
    TSecAuthService.withAuthorization(_allRoles[F, AugmentedJWT[Auth, Long]])(pf)

  def allRolesHandler[F[_], Auth](
      pf: PartialFunction[SecuredRequest[F, User, AugmentedJWT[Auth, Long]], F[Response[F]]],
  )(
      onNotAuthorized: TSecAuthService[User, AugmentedJWT[Auth, Long], F],
  )(implicit F: MonadError[F, Throwable]): TSecAuthService[User, AugmentedJWT[Auth, Long], F] =
    TSecAuthService.withAuthorizationHandler(_allRoles[F, AugmentedJWT[Auth, Long]])(
      pf,
      onNotAuthorized.run,
    )

  private def _adminOnly[F[_], Auth](implicit
      F: MonadError[F, Throwable],
  ): BasicRBAC[F, Role, User, Auth] =
    BasicRBAC[F, Role, User, Auth](Role.Admin)

  def adminOnly[F[_], Auth](
      pf: PartialFunction[SecuredRequest[F, User, AugmentedJWT[Auth, Long]], F[Response[F]]],
  )(implicit F: MonadError[F, Throwable]): TSecAuthService[User, AugmentedJWT[Auth, Long], F] =
    TSecAuthService.withAuthorization(_adminOnly[F, AugmentedJWT[Auth, Long]])(pf)
}

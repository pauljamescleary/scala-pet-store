package io.github.pauljamescleary.petstore.domain.authentication

import cats.effect._
import io.github.pauljamescleary.petstore.domain.users.User
import tsec.authentication.{AugmentedJWT, BackingStore, IdentityStore, JWTAuthenticator}
import tsec.common.SecureRandomId
import tsec.jws.mac.JWSMacCV
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.mac.jca.MacSigningKey

import scala.concurrent.duration._

object Auth {
  def jwtAuthenticator[F[_]: Sync, Auth: JWTMacAlgo](
      key: MacSigningKey[Auth],
      authRepo: BackingStore[F, SecureRandomId, AugmentedJWT[Auth, Long]],
      userRepo: IdentityStore[F, Long, User])(
      implicit cv: JWSMacCV[F, Auth]): JWTAuthenticator[F, Long, User, Auth] =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = 1.hour,
      maxIdle = None,
      tokenStore = authRepo,
      identityStore = userRepo,
      signingKey = key
    )
}

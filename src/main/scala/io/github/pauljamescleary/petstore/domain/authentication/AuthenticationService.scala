package io.github.pauljamescleary.petstore.domain.authentication

import cats.Id
import cats.data.OptionT
import cats.effect.{Effect, Sync}
import io.github.pauljamescleary.petstore.domain.users.{User, UserService}
import tsec.authentication.{AugmentedJWT, BackingStore, JWTAuthenticator, SecuredRequestHandler, TSecAuthService}
import tsec.common.SecureRandomId
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.collection.mutable
import scala.concurrent.duration._

class AuthenticationService[F[_] : Effect](userService: UserService[F]) {

  def memoryStore[I, V](getId: V => I) = new BackingStore[F, I, V] {

    val F = implicitly[Sync[F]]

    private val storageMap = mutable.HashMap.empty[I, V]

    def put(elem: V): F[V] = {
      val map = storageMap.put(getId(elem), elem)
      if (map.isEmpty) {
        F.pure(elem)
      } else {
        F.raiseError(new IllegalArgumentException)
      }
    }

    def get(id: I): OptionT[F, V] =
      OptionT.fromOption[F](storageMap.get(id))

    def update(v: V): F[V] = {
      storageMap.update(getId(v), v)
      F.pure(v)
    }

    def delete(id: I): F[Unit] =
      storageMap.remove(id) match {
        case Some(_) => F.unit
        case None => F.raiseError(new IllegalArgumentException)
      }
  }

  def backingStore(getId: User => Long) = new BackingStore[F, Long, User] {

    val F = implicitly[Effect[F]]

    override def put(elem: User): F[User] =
      F.flatMap(userService.createUser(elem).value) {
        case Right(user) => F.pure(user)
        case Left(error) => F.raiseError(new IllegalArgumentException(s"${error.user} already existed"))
      }

    override def update(user: User): F[User] =
      F.flatMap(userService.update(user).value) {
        case Right(u) => F.pure(u)
        case Left(error) => F.raiseError(new IllegalArgumentException(s"${error} not found"))
      }

    override def delete(id: Long): F[Unit] = userService.deleteUser(id)

    override def get(id: Long): OptionT[F, User] = OptionT(
      F.flatMap(userService.getUser(id).value) {
        case Right(user) => F.pure(Option(user))
        case Left(_) => F.pure(Option.empty[User])
      })
  }

  type AuthService = TSecAuthService[User, AugmentedJWT[HMACSHA256, Long], F]

  val jwtStore = memoryStore[SecureRandomId, AugmentedJWT[HMACSHA256, Long]](s => SecureRandomId.coerce(s.id))

  //FIXME: What am I doing here, id.get?
  val userStore: BackingStore[F, Long, User] = backingStore(_.id.get)

  val signingKey: MacSigningKey[HMACSHA256] = HMACSHA256.generateKey[Id]

  val jwtStatefulAuth: JWTAuthenticator[F, Long, User, HMACSHA256] =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = 10.minutes, //Absolute expiration time
      maxIdle = None,
      tokenStore = jwtStore,
      identityStore = userStore,
      signingKey = signingKey
    )

  val Auth =
    SecuredRequestHandler(jwtStatefulAuth)

}

object AuthenticationService {
  def apply[F[_] : Effect](userService: UserService[F]) = new AuthenticationService[F](userService)
}
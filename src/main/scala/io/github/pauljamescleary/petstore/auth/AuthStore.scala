package io.github.pauljamescleary.petstore.auth

import cats.data.OptionT
import cats.effect.Sync
import io.github.pauljamescleary.petstore.model.User
import io.github.pauljamescleary.petstore.repository.UserRepositoryAlgebra
import tsec.authentication._
import tsec.mac.imports.HMACSHA256

import scala.concurrent.duration._

object AuthStore {
  import cats.syntax.all._

  def backingStore[F[_]](userRepo: UserRepositoryAlgebra[F])(implicit F: Sync[F]) =
    F.pure(new BackingStore[F, Long, User] {
      override def put(elem: User): F[User] = userRepo.put(elem)

      override def get(id: Long): OptionT[F, User] = OptionT.apply(userRepo.get(id))

      override def update(v: User): F[User] = userRepo.put(v)

      override def delete(id: Long): F[Unit] = userRepo.delete(id).map(_ => ())
    })

  def jwtAuthenticator[F[_]](userRepo: UserRepositoryAlgebra[F])(implicit F: Sync[F]) = {
    for {
      store <- backingStore(userRepo)
      key <- HMACSHA256.generateLift
    } yield {
      JWTAuthenticator.stateless(
        expiry = 10.minutes,
        maxIdle = None,
        identityStore = store,
        signingKey = key
      )
    }
  }
}

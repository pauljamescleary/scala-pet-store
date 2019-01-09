package io.github.pauljamescleary.petstore.config

import cats.ApplicativeError
import io.circe.generic.auto._

final case class ServerConfig(host: String, port: Int)
final case class PetStoreConfig(db: DatabaseConfig, server: ServerConfig)

object PetStoreConfig {
  /**
    * Loads the pet store config using PureConfig.  If configuration is invalid we will
    * return an error.  This should halt the application from starting up.
    */
  def load[F[_]](implicit ev: ApplicativeError[F, Throwable]): F[PetStoreConfig] =
    loadConfigF[F, PetStoreConfig]("petstore")
}

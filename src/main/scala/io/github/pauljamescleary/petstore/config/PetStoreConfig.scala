package io.github.pauljamescleary.petstore.config

import cats.effect.Sync
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

case class PetStoreConfig(db: DatabaseConfig)

object PetStoreConfig {

  /**
    * Loads the pet store config using PureConfig.  If configuration is invalid we will
    * return an error.  This should halt the application from starting up.
    */
  def load[F[_]](implicit E: Sync[F]): F[PetStoreConfig] = loadConfigF[F, PetStoreConfig]("petstore")
}

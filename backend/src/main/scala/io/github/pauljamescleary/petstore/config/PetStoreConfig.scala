package io.github.pauljamescleary.petstore.config

import cats.effect.Sync
import cats.implicits._
import pureconfig.error.ConfigReaderException

case class PetStoreConfig(db: DatabaseConfig)

object PetStoreConfig {

  import pureconfig._

  /**
    * Loads the pet store config using PureConfig.  If configuration is invalid we will
    * return an error.  This should halt the application from starting up.
    */
  def load[F[_]](implicit E: Sync[F]): F[PetStoreConfig] =
    E.delay(loadConfig[PetStoreConfig]("petstore")).flatMap {
      case Right(ok) => E.pure(ok)
      case Left(e) => E.raiseError(new ConfigReaderException[PetStoreConfig](e))
    }
}

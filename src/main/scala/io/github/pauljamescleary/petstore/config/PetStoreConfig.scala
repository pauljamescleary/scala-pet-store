package io.github.pauljamescleary.petstore.config

import cats.effect.IO
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto._

final case class ServerConfig(host: String, port: Int)
object ServerConfig {
  implicit val configReader: ConfigReader[ServerConfig] = deriveReader
}
final case class PetStoreConfig(db: DatabaseConfig, server: ServerConfig)
object PetStoreConfig {
  implicit val configReader: ConfigReader[PetStoreConfig] = deriveReader

  val load: IO[PetStoreConfig] = IO.delay {
    ConfigSource.default.at("petstore").loadOrThrow[PetStoreConfig]
  }
}

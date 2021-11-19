package io.github.pauljamescleary.petstore.config

import cats.effect.{Blocker, ContextShift, IO, Resource}
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

import scala.concurrent.ExecutionContext

case class DatabaseConnectionsConfig(poolSize: Int)
object DatabaseConnectionsConfig {
  implicit val configReader: ConfigReader[DatabaseConnectionsConfig] = deriveReader
}
case class DatabaseConfig(
    url: String,
    driver: String,
    user: String,
    password: String,
    connections: DatabaseConnectionsConfig,
)

object DatabaseConfig {
  implicit val configReader: ConfigReader[DatabaseConfig] = deriveReader

  def dbTransactor(
      dbc: DatabaseConfig,
      connEc: ExecutionContext,
      blocker: Blocker,
  )(implicit cs: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] =
    HikariTransactor
      .newHikariTransactor[IO](dbc.driver, dbc.url, dbc.user, dbc.password, connEc, blocker)

  /**
    * Runs the flyway migrations against the target database
    */
  def initializeDb(cfg: DatabaseConfig): IO[Unit] =
    IO.delay {
      val fw: Flyway =
        Flyway
          .configure()
          .dataSource(cfg.url, cfg.user, cfg.password)
          .load()
      fw.migrate()
    }.as(())
}

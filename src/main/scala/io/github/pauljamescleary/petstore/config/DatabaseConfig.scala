package io.github.pauljamescleary.petstore.config

import cats.effect.{Async, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

case class DatabaseConfig(url: String, driver: String, user: String, password: String)

object DatabaseConfig {
  def dbTransactor[F[_]: Async : ContextShift](
    dbc: DatabaseConfig,
    connEc : ExecutionContext,
    transEc : ExecutionContext
  ): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](dbc.driver, dbc.url, dbc.user, dbc.password, connEc, transEc)

  /**
    * Runs the flyway migrations against the target database
    */
  def
  initializeDb[F[_]](cfg : DatabaseConfig)(implicit S: Sync[F]): F[Unit] =
    S.delay {
      val fw = new Flyway()
      fw.setDataSource(cfg.url, cfg.user, cfg.password)
      fw.migrate()
      ()
    }
}

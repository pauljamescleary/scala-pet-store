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
    *
    * This only gets applied if the database is H2, our local in-memory database.  Otherwise
    * we skip this step
    */
  def initializeDb[F[_]](dbConfig: DatabaseConfig)(implicit S: Sync[F]): F[Unit] =
    if (dbConfig.url.contains(":h2:")) {
      S.delay {
        val fw = new Flyway()
        val ds = new org.h2.jdbcx.JdbcDataSource()
        ds.setUrl(dbConfig.url)
        ds.setUser(dbConfig.user)
        ds.setPassword(dbConfig.password)
        fw.setDataSource(ds)
        fw.migrate()
        ()
      }
    } else {
      S.pure(())
    }
}

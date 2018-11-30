package io.github.pauljamescleary.petstore.infrastructure.repository

import cats.implicits._
import cats.effect.{Async, ContextShift, Effect, IO}
import io.github.pauljamescleary.petstore.config.{DatabaseConfig, PetStoreConfig}
import _root_.doobie.Transactor
import javax.sql.DataSource

import scala.concurrent.ExecutionContext

package object doobie {
  def getTransactor[F[_] : Async : ContextShift](cfg : DatabaseConfig) : Transactor[F] =
    Transactor.fromDriverManager[F](
      cfg.driver, // driver classname
      cfg.url, // connect URL (driver-specific)
      cfg.user,              // user
      cfg.password           // password
    )

  /*
   * Create an h2 DataSource which flyway can use to initialize the database.
   */
  def h2DataSource(cfg : DatabaseConfig) : DataSource = {
    val ds = new org.h2.jdbcx.JdbcDataSource()
    ds.setUrl(cfg.url)
    ds.setUser(cfg.user)
    ds.setPassword(cfg.password)
    ds
  }

  /*
   * Provide a transactor for testing once schema has been migrated.
   */
  def initializedTransactor[F[_] : Effect : Async : ContextShift] : F[Transactor[F]] = for {
    petConfig <- PetStoreConfig.load[F]
    _ <- DatabaseConfig.initializeDb(h2DataSource(petConfig.db))
  } yield getTransactor(petConfig.db)

  lazy val testEc = ExecutionContext.Implicits.global

  implicit lazy val testCs = IO.contextShift(testEc)

  lazy val testTransactor = initializedTransactor[IO].unsafeRunSync()
}

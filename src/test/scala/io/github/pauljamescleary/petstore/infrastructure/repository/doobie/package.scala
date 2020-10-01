package io.github.pauljamescleary.petstore
package infrastructure.repository

import cats.syntax.all._
import cats.effect.{Async, ContextShift, Effect, IO}
import config._
import _root_.doobie.Transactor
import io.circe.config.parser

import scala.concurrent.ExecutionContext

package object doobie {
  def getTransactor[F[_]: Async: ContextShift](cfg: DatabaseConfig): Transactor[F] =
    Transactor.fromDriverManager[F](
      cfg.driver, // driver classname
      cfg.url, // connect URL (driver-specific)
      cfg.user, // user
      cfg.password, // password
    )

  /*
   * Provide a transactor for testing once schema has been migrated.
   */
  def initializedTransactor[F[_]: Effect: Async: ContextShift]: F[Transactor[F]] =
    for {
      petConfig <- parser.decodePathF[F, PetStoreConfig]("petstore")
      _ <- DatabaseConfig.initializeDb(petConfig.db)
    } yield getTransactor(petConfig.db)

  lazy val testEc = ExecutionContext.Implicits.global

  implicit lazy val testCs = IO.contextShift(testEc)

  lazy val testTransactor = initializedTransactor[IO].unsafeRunSync()
}

package io.github.pauljamescleary.petstore.infrastructure.repository

import cats.effect.IO
import io.github.pauljamescleary.petstore.config.{DatabaseConfig, PetStoreConfig}
import _root_.doobie.Transactor

package object doobie {
  def getTransactor : IO[Transactor[IO]] = for {
    conf <- PetStoreConfig.load[IO]
    tr <- DatabaseConfig.dbTransactor[IO](conf.db)
    x <- DatabaseConfig.initializeDb[IO](conf.db)
  } yield tr

  lazy val testTransactor : Transactor[IO] = getTransactor.unsafeRunSync()
}

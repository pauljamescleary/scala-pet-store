package io.github.pauljamescleary.petstore

import cats.effect._
import cats.implicits._
import fs2.Stream
import io.github.pauljamescleary.petstore.config.{DatabaseConfig, PetStoreConfig}
import io.github.pauljamescleary.petstore.endpoint.{OrderEndpoints, PetEndpoints}
import io.github.pauljamescleary.petstore.repository.{
  DoobieOrderRepositoryInterpreter,
  DoobiePetRepositoryInterpreter
}
import io.github.pauljamescleary.petstore.service.{OrderService, PetService}
import io.github.pauljamescleary.petstore.validation.PetValidationInterpreter
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp
import org.http4s.util.StreamApp.ExitCode

object Server extends StreamApp[IO] {

  override def stream(args: List[String], shutdown: IO[Unit]): Stream[IO, ExitCode] =
    createStream[IO](args, shutdown).unsafeRunSync()

  def createStream[F[_]](args: List[String], shutdown: F[Unit])(
      implicit E: Effect[F]): F[Stream[F, ExitCode]] =
    for {
      conf <- PetStoreConfig.load[F]
      xa <- DatabaseConfig.dbTransactor(conf.db)
      _ <- DatabaseConfig.initializeDb(conf.db, xa)
      petRepo = DoobiePetRepositoryInterpreter[F](xa)
      orderRepo = DoobieOrderRepositoryInterpreter[F](xa)
    } yield {

      val petValidation = PetValidationInterpreter[F](petRepo)
      val petService = PetService[F](petRepo, petValidation)
      val orderService = OrderService[F](orderRepo)

      BlazeBuilder[F]
        .bindHttp(8080, "localhost")
        .mountService(PetEndpoints.endpoints[F](petService), "/")
        .mountService(OrderEndpoints.endpoints[F](orderService), "/")
        .serve
    }
}

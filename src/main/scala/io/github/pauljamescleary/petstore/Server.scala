package io.github.pauljamescleary.petstore

import cats.effect._
import cats.implicits._
import doobie.h2.H2Transactor
import fs2.Stream
import io.github.pauljamescleary.petstore.endpoint.{OrderEndpoints, PetEndpoints}
import io.github.pauljamescleary.petstore.repository.{
  DoobieOrderRepositoryInterpreter,
  DoobiePetRepositoryInterpreter
}
import io.github.pauljamescleary.petstore.service.{OrderService, PetService}
import io.github.pauljamescleary.petstore.validation.PetValidationInterpreter
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp

object Server extends StreamApp[IO] {

  override def stream(args: List[String], shutdown: IO[Unit]): Stream[IO, Nothing] =
    createStream[IO](args, shutdown).unsafeRunSync()

  def createStream[F[_]](args: List[String], shutdown: F[Unit])(
      implicit E: Effect[F]): F[Stream[F, Nothing]] =
    for {
      xa <- H2Transactor[F]("jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")
      petRepo = DoobiePetRepositoryInterpreter[F](xa)
      orderRepo = DoobieOrderRepositoryInterpreter[F](xa)
      _ <- petRepo.migrate
      _ <- orderRepo.migrate
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

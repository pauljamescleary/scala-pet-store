package io.github.pauljamescleary.petstore

import fs2.Stream
import cats.effect.IO
import cats.effect._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp
import io.github.pauljamescleary.petstore.repository.{DoobieOrderRepositoryInterpreter, DoobiePetRepositoryInterpreter, PetRepositoryInMemoryInterpreter}
import io.github.pauljamescleary.petstore.validation.PetValidationTaskInterpreter
import io.github.pauljamescleary.petstore.service.{OrderService, PetService}
import io.github.pauljamescleary.petstore.endpoint.{OrderEndpoints, PetEndpoints}

object Server extends StreamApp[IO] {

  private implicit val petRepo = DoobiePetRepositoryInterpreter()
  private implicit val petValidation = new PetValidationTaskInterpreter
  private implicit val petService = new PetService[IO]
  private implicit val orderRepo = DoobieOrderRepositoryInterpreter()
  private implicit val orderService = new OrderService[IO]

  override def stream(args: List[String], shutdown: IO[Unit]): Stream[IO, Nothing] = {
    BlazeBuilder[IO]
      .bindHttp(8080, "localhost")
      .mountService(PetEndpoints.endpoints(petService), "/")
      .mountService(OrderEndpoints.endpoints(orderService), "/")
      .serve
  }
}

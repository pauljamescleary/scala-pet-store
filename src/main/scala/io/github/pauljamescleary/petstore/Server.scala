package io.github.pauljamescleary.petstore

import cats.effect.IO
import fs2.Stream
import io.github.pauljamescleary.petstore.endpoint.{OrderEndpoints, PetEndpoints}
import io.github.pauljamescleary.petstore.repository.{DoobieOrderRepositoryInterpreter, DoobiePetRepositoryInterpreter}
import io.github.pauljamescleary.petstore.service.{OrderService, PetService}
import io.github.pauljamescleary.petstore.validation.PetValidationInterpreter
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp

object Server extends StreamApp[IO] {

  private implicit val petRepo = DoobiePetRepositoryInterpreter()
  private implicit val petValidation = new PetValidationInterpreter
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

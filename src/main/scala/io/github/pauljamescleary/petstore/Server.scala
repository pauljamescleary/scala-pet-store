package io.github.pauljamescleary.petstore

import cats.effect.IO
import fs2.Stream
import io.github.pauljamescleary.petstore.endpoint.{
  OrderEndpoints,
  PetEndpoints
}
import io.github.pauljamescleary.petstore.repository.{
  DoobieOrderRepositoryInterpreter,
  DoobiePetRepositoryInterpreter
}
import io.github.pauljamescleary.petstore.service.{OrderService, PetService}
import io.github.pauljamescleary.petstore.validation.PetValidationInterpreter
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp

object Server extends StreamApp[IO] {

  private val petRepo = DoobiePetRepositoryInterpreter()
  private val petValidation = new PetValidationInterpreter(petRepo)
  private val petService = new PetService[IO](petRepo, petValidation)
  private val orderRepo = DoobieOrderRepositoryInterpreter()
  private val orderService = new OrderService[IO](orderRepo)

  override def stream(args: List[String],
                      shutdown: IO[Unit]): Stream[IO, Nothing] = {
    BlazeBuilder[IO]
      .bindHttp(8080, "localhost")
      .mountService(PetEndpoints.endpoints(petService), "/")
      .mountService(OrderEndpoints.endpoints(orderService), "/")
      .serve
  }
}

package io.github.pauljamescleary.petstore

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp
import io.github.pauljamescleary.petstore.repository.{DoobieOrderRepositoryInterpreter, DoobiePetRepositoryInterpreter, PetRepositoryInMemoryInterpreter}
import io.github.pauljamescleary.petstore.validation.PetValidationTaskInterpreter
import io.github.pauljamescleary.petstore.service.{OrderService, PetService}
import io.github.pauljamescleary.petstore.endpoint.{OrderEndpoints, PetEndpoints}
import fs2._

object Server extends StreamApp {

  private implicit val petRepo = DoobiePetRepositoryInterpreter()
  private implicit val petValidation = new PetValidationTaskInterpreter
  private implicit val petService = new PetService[Task]
  private implicit val orderRepo = DoobieOrderRepositoryInterpreter()
  private implicit val orderService = new OrderService[Task]

  override def stream(args: List[String]): Stream[Task, Nothing] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(PetEndpoints.endpoints(petService), "/")
      .mountService(OrderEndpoints.endpoints(orderService), "/")
      .serve
  }
}

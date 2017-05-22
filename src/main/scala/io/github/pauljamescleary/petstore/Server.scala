package io.github.pauljamescleary.petstore

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp
import io.github.pauljamescleary.petstore.repository.PetRepositoryInMemoryInterpreter
import io.github.pauljamescleary.petstore.validation.PetValidationTaskInterpreter
import io.github.pauljamescleary.petstore.service.PetService
import io.github.pauljamescleary.petstore.endpoint.PetEndpoints

import fs2._

object Server extends StreamApp {

  private implicit val petRepo = PetRepositoryInMemoryInterpreter
  private implicit val petValidation = new PetValidationTaskInterpreter
  private implicit val petService = new PetService[Task]

  override def stream(args: List[String]): Stream[Task, Nothing] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(PetEndpoints.endpoints(petService), "/")
      .serve
  }
}

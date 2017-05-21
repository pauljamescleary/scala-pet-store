package io.github.pauljamescleary.petstore

import fs2._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp

object Server extends StreamApp {

  private implicit val petRepo = PetRepositoryTaskIntepreter
  private implicit val petValidation = new PetValidationTaskInterpreter
  private implicit val petService = new PetService[Task]

  override def stream(args: List[String]): Stream[Task, Nothing] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(PetEndpoints.endpoints(petService), "/")
      .serve
  }
}

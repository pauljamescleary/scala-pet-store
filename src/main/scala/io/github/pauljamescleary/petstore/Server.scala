package io.github.pauljamescleary.petstore

import fs2._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp

object Server extends StreamApp {

  private val petService = new PetService[Task](PetRepositoryTaskIntepreter)

  override def stream(args: List[String]): Stream[Task, Nothing] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(PetEndpoints.endpoints(petService), "/")
      .serve
  }
}

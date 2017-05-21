package io.github.pauljamescleary.petstore

import fs2._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp

object BlazeExample extends StreamApp {

  override def stream(args: List[String]): Stream[Task, Nothing] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(HelloWorld.service, "/")
      .serve
  }
}

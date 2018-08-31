package io.github.pauljamescleary.petstore.infrastructure.endpoint

import cats.effect.Effect
import org.http4s.{HttpService, Request, StaticFile}
import org.http4s.dsl.Http4sDsl

class SiteEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  def indexEndPoint: HttpService[F] = HttpService[F] {
    case req @ GET -> Root / path if List(".js", ".css", ".map", ".html", ".img").exists(path.endsWith) =>
      static(path, req)
  }

  private def static(file: String, request: Request[F]) =
    StaticFile.fromResource("/" + file, Some(request)).getOrElseF(NotFound())

  def endpoints: HttpService[F] = indexEndPoint

}

object SiteEndpoints {
  def endpoints[F[_]: Effect]: HttpService[F] = new SiteEndpoints[F].endpoints
}

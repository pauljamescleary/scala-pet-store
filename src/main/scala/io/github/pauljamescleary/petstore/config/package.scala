package io.github.pauljamescleary.petstore

import cats.ApplicativeError
import cats.implicits._
import com.typesafe.config.ConfigFactory
import io.circe.Decoder
import io.circe.config.syntax._

package object config {
  def loadConfigF[F[_], C : Decoder](name: String = "")(implicit ev: ApplicativeError[F, Throwable]) : F[C] =
    ConfigFactory.load().as[C](name).leftWiden[Throwable].raiseOrPure[F]
}

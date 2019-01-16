package io.github.pauljamescleary.petstore

import cats.ApplicativeError
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Decoder
import io.circe.config.syntax._

package object config {
  def loadSafe() : Either[Throwable, Config] =
    Either.catchNonFatal(ConfigFactory.load())

  def loadConfigF[F[_], C : Decoder](implicit ev: ApplicativeError[F, Throwable]) : F[C] =
    loadSafe().flatMap(_.as[C].leftWiden[Throwable]).raiseOrPure[F]

  def loadConfigF[F[_], C : Decoder](name: String = "")(implicit ev: ApplicativeError[F, Throwable]) : F[C] =
    loadSafe().flatMap(_.as[C](name).leftWiden[Throwable]).raiseOrPure[F]
}

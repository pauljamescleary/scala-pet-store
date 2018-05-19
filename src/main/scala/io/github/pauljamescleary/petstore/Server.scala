package io.github.pauljamescleary.petstore

import config.{DatabaseConfig, PetStoreConfig}
import domain.users._
import domain.orders._
import domain.pets._
import infrastructure.endpoint.{OrderEndpoints, PetEndpoints, UserEndpoints}
import infrastructure.repository.doobie.{DoobieOrderRepositoryInterpreter, DoobiePetRepositoryInterpreter, DoobieUserRepositoryInterpreter}
import cats.effect._
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze.BlazeBuilder
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt

object Server extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  override def stream(args: List[String], shutdown: IO[Unit]): Stream[IO, ExitCode] =
    createStream[IO](args, shutdown)

  private val keyGen = HMACSHA256

  def createStream[F[_]](args: List[String], shutdown: F[Unit])(
      implicit E: Effect[F]): Stream[F, ExitCode] =
    for {
      conf           <- Stream.eval(PetStoreConfig.load[F])
      signingKey     <- Stream.eval(keyGen.generateKey[F])
      xa             <- Stream.eval(DatabaseConfig.dbTransactor(conf.db))
      _              <- Stream.eval(DatabaseConfig.initializeDb(conf.db, xa))
      petRepo        =  DoobiePetRepositoryInterpreter[F](xa)
      orderRepo      =  DoobieOrderRepositoryInterpreter[F](xa)
      userRepo       =  DoobieUserRepositoryInterpreter[F](xa)
      petValidation  =  PetValidationInterpreter[F](petRepo)
      petService     =  PetService[F](petRepo, petValidation)
      userValidation =  UserValidationInterpreter[F](userRepo)
      orderService   =  OrderService[F](orderRepo)
      userService    =  UserService[F](userRepo, userValidation)
      exitCode       <- BlazeBuilder[F]
        .bindHttp(8080, "localhost")
        .mountService(PetEndpoints.endpoints[F](petService), "/")
        .mountService(OrderEndpoints.endpoints[F](orderService), "/")
        .mountService(UserEndpoints.endpoints(userService, BCrypt.syncPasswordHasher[F]), "/")
        .serve
    } yield exitCode
}

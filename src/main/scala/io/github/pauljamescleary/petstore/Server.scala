package io.github.pauljamescleary.petstore

import config.{DatabaseConfig, PetStoreConfig}
import domain.users._
import domain.orders._
import domain.pets._
import infrastructure.endpoint.{OrderEndpoints, PetEndpoints, UserEndpoints}
import infrastructure.repository.doobie.{DoobieOrderRepositoryInterpreter, DoobiePetRepositoryInterpreter, DoobieUserRepositoryInterpreter}
import cats.effect._
import cats.implicits._
import fs2._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import scala.concurrent.ExecutionContext.Implicits.global

object Server extends IOApp {
  private val keyGen = HMACSHA256

  def createStream[F[_] : ContextShift : ConcurrentEffect : Timer](args: List[String]): Stream[F, ExitCode] =
    for {
      conf           <- Stream.eval(PetStoreConfig.load[F])
      signingKey     <- Stream.eval(keyGen.generateKey[F])
      _              <- Stream.eval(DatabaseConfig.initializeDb(conf.db))
      xar            <- Stream(DatabaseConfig.dbTransactor(conf.db, global, global)).covary[F]
      httpApp        <- Stream.eval(xar.use{ xa =>
        val petRepo        =  DoobiePetRepositoryInterpreter[F](xa)
        val orderRepo      =  DoobieOrderRepositoryInterpreter[F](xa)
        val userRepo       =  DoobieUserRepositoryInterpreter[F](xa)
        val petValidation  =  PetValidationInterpreter[F](petRepo)
        val petService     =  PetService[F](petRepo, petValidation)
        val userValidation =  UserValidationInterpreter[F](userRepo)
        val orderService   =  OrderService[F](orderRepo)
        val userService    =  UserService[F](userRepo, userValidation)
        val services       =  PetEndpoints.endpoints[F](petService) <+>
                              OrderEndpoints.endpoints[F](orderService) <+>
                              UserEndpoints.endpoints[F, BCrypt](userService, BCrypt.syncPasswordHasher[F])
        Router("/" -> services).orNotFound.pure[F]
      })
      exitCode       <- BlazeServerBuilder[F]
                          .bindHttp(8080, "localhost")
                          .withHttpApp(httpApp)
                          .serve
    } yield exitCode

  def run(args : List[String]) : IO[ExitCode] = createStream[IO](args).compile.drain.as(ExitCode.Success)
}

package io.github.pauljamescleary.petstore

import config.{DatabaseConfig, PetStoreConfig}
import domain.users._
import domain.orders._
import domain.pets._
import infrastructure.endpoint.{OrderEndpoints, PetEndpoints, UserEndpoints}
import infrastructure.repository.doobie.{DoobieOrderRepositoryInterpreter, DoobiePetRepositoryInterpreter, DoobieUserRepositoryInterpreter}
import cats.effect._
import cats.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import scala.concurrent.ExecutionContext.Implicits.global

object Server extends IOApp {
  private val keyGen = HMACSHA256

  def createStream[F[_] : ContextShift : ConcurrentEffect : Timer]: F[ExitCode] =
    for {
      conf           <- PetStoreConfig.load[F]
      signingKey     <- keyGen.generateKey[F]
      _              <- DatabaseConfig.initializeDb(conf.db)
      xar            =  DatabaseConfig.dbTransactor(conf.db, global, global)
      exitCode       <- xar.use{ xa =>
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
        val httpApp = Router("/" -> services).orNotFound
        BlazeServerBuilder[F]
        .bindHttp(8080, "localhost")
        .withHttpApp(httpApp)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
      }
    } yield exitCode

  def run(args : List[String]) : IO[ExitCode] = createStream[IO]
}

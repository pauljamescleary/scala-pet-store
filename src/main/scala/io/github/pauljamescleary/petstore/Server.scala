package io.github.pauljamescleary.petstore

import config.{DatabaseConfig, PetStoreConfig}
import domain.users._
import domain.orders._
import domain.pets._
import infrastructure.endpoint.{OrderEndpoints, PetEndpoints, UserEndpoints}
import infrastructure.repository.doobie.{DoobieOrderRepositoryInterpreter, DoobiePetRepositoryInterpreter, DoobieUserRepositoryInterpreter}
import cats.effect._
import cats.implicits._
import org.http4s.server.{Server => H4Server, Router}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import scala.concurrent.ExecutionContext.Implicits.global

object Server extends IOApp {
  private val keyGen = HMACSHA256

  def createServer[F[_] : ContextShift : ConcurrentEffect : Timer]: Resource[F, H4Server[F]] =
    for {
      conf           <- Resource.liftF(PetStoreConfig.load[F])
      signingKey     <- Resource.liftF(keyGen.generateKey[F])
      xa             <- DatabaseConfig.dbTransactor(conf.db, global, global)
      petRepo        =  DoobiePetRepositoryInterpreter[F](xa)
      orderRepo      =  DoobieOrderRepositoryInterpreter[F](xa)
      userRepo       =  DoobieUserRepositoryInterpreter[F](xa)
      petValidation  =  PetValidationInterpreter[F](petRepo)
      petService     =  PetService[F](petRepo, petValidation)
      userValidation =  UserValidationInterpreter[F](userRepo)
      orderService   =  OrderService[F](orderRepo)
      userService    =  UserService[F](userRepo, userValidation)
      services       =  PetEndpoints.endpoints[F](petService) <+>
                            OrderEndpoints.endpoints[F](orderService) <+>
                            UserEndpoints.endpoints[F, BCrypt](userService, BCrypt.syncPasswordHasher[F])
      httpApp = Router("/" -> services).orNotFound
      _ <- Resource.liftF(DatabaseConfig.initializeDb(conf.db))
      server <-
        BlazeServerBuilder[F]
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args : List[String]) : IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}

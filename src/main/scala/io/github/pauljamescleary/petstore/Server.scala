package io.github.pauljamescleary.petstore

import config._
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
import tsec.passwordhashers.jca.BCrypt
import doobie.util.ExecutionContexts
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.config.parser

object Server extends IOApp {
  def createServer[F[_] : ContextShift : ConcurrentEffect : Timer]: Resource[F, H4Server[F]] =
    for {
      conf           <- Resource.liftF(parser.decodePathF[F, PetStoreConfig]("petstore"))
      connEc         <- ExecutionContexts.fixedThreadPool[F](10)
      xa             <- DatabaseConfig.dbTransactor(conf.db, connEc, global)
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

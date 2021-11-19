package io.github.pauljamescleary.petstore

import config._
import domain.users._
import domain.orders._
import domain.pets._
import infrastructure.endpoint._
import infrastructure.repository.doobie.{
  DoobieAuthRepositoryInterpreter,
  DoobieOrderRepositoryInterpreter,
  DoobiePetRepositoryInterpreter,
  DoobieUserRepositoryInterpreter,
}
import cats.effect._
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.passwordhashers.jca.BCrypt
import doobie.util.ExecutionContexts
import io.circe.config.parser
import domain.authentication.Auth
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256

object Server extends IOApp {
  val createServer: Resource[IO, H4Server[IO]] =
    for {
      conf <- Resource.eval(PetStoreConfig.load)
      serverEc <- ExecutionContexts.cachedThreadPool[IO]
      connEc <- ExecutionContexts.fixedThreadPool[IO](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[IO]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      key <- Resource.eval(HMACSHA256.generateKey[IO])
      authRepo = DoobieAuthRepositoryInterpreter[HMACSHA256](key, xa)
      petRepo = DoobiePetRepositoryInterpreter(xa)
      orderRepo = DoobieOrderRepositoryInterpreter(xa)
      userRepo = DoobieUserRepositoryInterpreter(xa)
      petValidation = PetValidationInterpreter(petRepo)
      petService = PetService(petRepo, petValidation)
      userValidation = UserValidationInterpreter(userRepo)
      orderService = OrderService(orderRepo)
      userService = UserService(userRepo, userValidation)
      authenticator = Auth.jwtAuthenticator[HMACSHA256](key, authRepo, userRepo)
      routeAuth = SecuredRequestHandler(authenticator)
      httpApp = Router(
        "/users" -> UserEndpoints
          .endpoints[BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[IO], routeAuth),
        "/pets" -> PetEndpoints.endpoints[HMACSHA256](petService, routeAuth),
        "/orders" -> OrderEndpoints.endpoints[HMACSHA256](orderService, routeAuth),
      ).orNotFound
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      server <- BlazeServerBuilder[IO](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}

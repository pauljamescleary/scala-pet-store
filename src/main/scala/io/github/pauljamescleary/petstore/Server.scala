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
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.liftF(parser.decodePathF[F, PetStoreConfig]("petstore"))
      serverEc <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      key <- Resource.liftF(HMACSHA256.generateKey[F])
      authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)
      petRepo = DoobiePetRepositoryInterpreter[F](xa)
      orderRepo = DoobieOrderRepositoryInterpreter[F](xa)
      userRepo = DoobieUserRepositoryInterpreter[F](xa)
      petValidation = PetValidationInterpreter[F](petRepo)
      petService = PetService[F](petRepo, petValidation)
      userValidation = UserValidationInterpreter[F](userRepo)
      orderService = OrderService[F](orderRepo)
      userService = UserService[F](userRepo, userValidation)
      authenticator = Auth.jwtAuthenticator[F, HMACSHA256](key, authRepo, userRepo)
      routeAuth = SecuredRequestHandler(authenticator)
      httpApp = Router(
        "/users" -> UserEndpoints
          .endpoints[F, BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[F], routeAuth),
        "/pets" -> PetEndpoints.endpoints[F, HMACSHA256](petService, routeAuth),
        "/orders" -> OrderEndpoints.endpoints[F, HMACSHA256](orderService, routeAuth),
      ).orNotFound
      _ <- Resource.liftF(DatabaseConfig.initializeDb(conf.db))
      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}

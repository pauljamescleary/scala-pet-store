package io.github.pauljamescleary.petstore.infrastructure.endpoint

import cats.effect.Effect
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds
import io.github.pauljamescleary.petstore.domain.OrderNotFoundError
import io.github.pauljamescleary.petstore.domain.orders.{Order, OrderService}
import io.github.pauljamescleary.petstore.domain.users.User
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler, TSecAuthService, asAuthed}
import tsec.jwt.algorithms.JWTMacAlgo

class OrderEndpoints[F[_]: Effect, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  /* Need Instant Json Encoding */
  import io.circe.java8.time._

  /* Needed for service composition via |+| */
  import cats.implicits._

  /* Needed to decode entities */
  implicit val orderDecoder = jsonOf[F, Order]

  type AuthService = TSecAuthService[User, AugmentedJWT[Auth, Long], F]

  def placeOrderEndpoint(orderService: OrderService[F]): AuthService =
    TSecAuthService {
      case req @ POST -> Root / "orders" asAuthed _ =>
        for {
          order <- req.request.as[Order]
          saved <- orderService.placeOrder(order)
          resp <- Ok(saved.asJson)
        } yield resp
    }

  private def getOrderEndpoint(orderService: OrderService[F]): AuthService =
    TSecAuthService {
      case GET -> Root / "orders" / LongVar(id) asAuthed _ =>
        orderService.get(id).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(OrderNotFoundError) => NotFound("The order was not found")
        }
    }

  private def deleteOrderEndpoint(orderService: OrderService[F]): AuthService =
    TSecAuthService {
      case DELETE -> Root / "orders" / LongVar(id) asAuthed _=>
        for {
          _ <- orderService.delete(id)
          resp <- Ok()
        } yield resp
    }

  def endpoints(orderService: OrderService[F],
                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]): HttpRoutes[F] = {
    val authEndpoints: AuthService  =
      placeOrderEndpoint(orderService) <+>
      getOrderEndpoint(orderService) <+>
      deleteOrderEndpoint(orderService)

    auth.liftService(authEndpoints)
  }
}

object OrderEndpoints {
  def endpoints[F[_]: Effect, Auth: JWTMacAlgo](orderService: OrderService[F],
                              auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]): HttpRoutes[F] =
    new OrderEndpoints[F, Auth].endpoints(orderService, auth)
}

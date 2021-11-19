package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.domain.OrderNotFoundError
import io.github.pauljamescleary.petstore.domain.authentication.Auth
import io.github.pauljamescleary.petstore.domain.orders.{Order, OrderService}
import io.github.pauljamescleary.petstore.domain.users.User
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler, asAuthed}
import tsec.jwt.algorithms.JWTMacAlgo

class OrderEndpoints[Auth: JWTMacAlgo] extends Http4sDsl[IO] {
  /* Needed to decode entities */
  implicit val orderDecoder: EntityDecoder[IO, Order] = jsonOf

  private def placeOrderEndpoint(orderService: OrderService): AuthEndpoint[IO, Auth] = {
    case req @ POST -> Root asAuthed user =>
      for {
        order <- req.request
          .as[Order]
          .map(_.copy(userId = user.id))
        saved <- orderService.placeOrder(order)
        resp <- Ok(saved.asJson)
      } yield resp
  }

  private def getOrderEndpoint(orderService: OrderService): AuthEndpoint[IO, Auth] = {
    case GET -> Root / LongVar(id) asAuthed _ =>
      orderService.get(id).flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(OrderNotFoundError) => NotFound("The order was not found")
      }
  }

  private def deleteOrderEndpoint(orderService: OrderService): AuthEndpoint[IO, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- orderService.delete(id)
        resp <- Ok()
      } yield resp
  }

  def endpoints(
      orderService: OrderService,
      auth: SecuredRequestHandler[IO, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[IO] = {
    val authEndpoints: AuthService[IO, Auth] =
      Auth.allRolesHandler(
        placeOrderEndpoint(orderService).orElse(getOrderEndpoint(orderService)),
      ) {
        Auth.adminOnly(deleteOrderEndpoint(orderService))
      }

    auth.liftService(authEndpoints)
  }
}

object OrderEndpoints {
  def endpoints[Auth: JWTMacAlgo](
      orderService: OrderService,
      auth: SecuredRequestHandler[IO, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[IO] =
    new OrderEndpoints[Auth].endpoints(orderService, auth)
}

package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import domain.OrderNotFoundError
import domain.authentication.Auth
import domain.orders.{Order, OrderService}
import io.github.pauljamescleary.petstore.domain.users.User
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler, asAuthed}
import tsec.jwt.algorithms.JWTMacAlgo

class OrderEndpoints[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  /* Needed to decode entities */
  implicit val orderDecoder: EntityDecoder[F, Order] = jsonOf

  private def placeOrderEndpoint(orderService: OrderService[F]): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed user =>
      for {
        order <- req.request
          .as[Order]
          .map(_.copy(userId = user.id))
        saved <- orderService.placeOrder(order)
        resp <- Ok(saved.asJson)
      } yield resp
  }

  private def getOrderEndpoint(orderService: OrderService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / LongVar(id) asAuthed _ =>
      orderService.get(id).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(OrderNotFoundError) => NotFound("The order was not found")
      }
  }

  private def deleteOrderEndpoint(orderService: OrderService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- orderService.delete(id)
        resp <- Ok()
      } yield resp
  }

  def endpoints(
      orderService: OrderService[F],
      auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] =
      Auth.allRolesHandler(
        placeOrderEndpoint(orderService).orElse(getOrderEndpoint(orderService)),
      ) {
        Auth.adminOnly(deleteOrderEndpoint(orderService))
      }

    auth.liftService(authEndpoints)
  }
}

object OrderEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](
      orderService: OrderService[F],
      auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] =
    new OrderEndpoints[F, Auth].endpoints(orderService, auth)
}

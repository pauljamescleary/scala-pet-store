package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import cats.effect.Effect
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import domain.OrderNotFoundError
import domain.orders.{Order, OrderService}

class OrderEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  /* Need Instant Json Encoding */
  import io.circe.java8.time._

  /* Needed for service composition via |+| */
  import cats.implicits._

  /* Needed to decode entities */
  implicit val orderDecoder = jsonOf[F, Order]

  def placeOrderEndpoint(orderService: OrderService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "orders" => {
        for {
          order <- req.as[Order]
          saved <- orderService.placeOrder(order)
          resp <- Ok(saved.asJson)
        } yield resp
      }
    }

  private def getOrderEndpoint(orderService: OrderService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "orders" / LongVar(id) =>
        orderService.get(id).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(OrderNotFoundError) => NotFound("The order was not found")
        }
    }

  private def deleteOrderEndpoint(orderService: OrderService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "orders" / LongVar(id) =>
        for {
          _ <- orderService.delete(id)
          resp <- Ok()
        } yield resp
    }

  def endpoints(orderService: OrderService[F]): HttpRoutes[F] =
    placeOrderEndpoint(orderService) <+> getOrderEndpoint(orderService) <+> deleteOrderEndpoint(orderService)
}

object OrderEndpoints {
  def endpoints[F[_]: Effect](orderService: OrderService[F]): HttpRoutes[F] =
    new OrderEndpoints[F].endpoints(orderService)
}

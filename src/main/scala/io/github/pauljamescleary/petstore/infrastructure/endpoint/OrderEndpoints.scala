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

class OrderEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  /* Need Instant Json Encoding */
  import io.circe.java8.time._

  /* Needed for service composition via |+| */
  import cats.implicits._

  /* Needed to decode entities */
  implicit val orderDecoder = jsonOf[F, Order]

  def placeOrderEndpoint(orderService: OrderService[F]): HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "orders" => {
        for {
          order <- req.as[Order]
          saved <- orderService.placeOrder(order)
          resp <- Ok(saved.asJson)
        } yield resp
      }
    }

  private def getOrderEndpoint(orderService: OrderService[F]): HttpService[F] =
    HttpService[F] {
      case GET -> Root / "orders" / LongVar(id) =>
        orderService.get(id).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(OrderNotFoundError) => NotFound("The order was not found")
        }
    }

  private def deleteOrderEndpoint(orderService: OrderService[F]): HttpService[F] =
    HttpService[F] {
      case DELETE -> Root / "orders" / LongVar(id) =>
        for {
          _ <- orderService.delete(id)
          resp <- Ok()
        } yield resp
    }

  def endpoints(orderService: OrderService[F]): HttpService[F] =
    placeOrderEndpoint(orderService) <+> getOrderEndpoint(orderService) <+> deleteOrderEndpoint(orderService)
}

object OrderEndpoints {
  def endpoints[F[_]: Effect](orderService: OrderService[F]): HttpService[F] =
    new OrderEndpoints[F].endpoints(orderService)
}

package io.github.pauljamescleary.petstore.endpoint

import cats.effect.Sync
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.model.{Order, OrderStatus}
import io.github.pauljamescleary.petstore.service.OrderService
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

class OrderEndpoints[F[_]: Sync] extends Http4sDsl[F] {

  /* Need Joda DateTime Json Encoding */
  import JodaDateTime._

  /* Needed for service composition via |+| */
  import cats.implicits._

  /* We need to define an enum encoder and decoder since these do not come out of the box with generic derivation */
  implicit val statusDecoder: Decoder[OrderStatus] = deriveEnumerationDecoder
  implicit val statusEncoder: Encoder[OrderStatus] = deriveEnumerationEncoder

  def placeOrderEndpoint(orderService: OrderService[F]): HttpService[F] = {
    HttpService[F] {
      case req @ POST -> Root / "orders" => {
        for {
          order <- req.decodeJson[Order]
          saved <- orderService.placeOrder(order)
          resp <- Ok(saved.asJson)
        } yield resp
      }
    }
  }

  def endpoints(orderService: OrderService[F]): HttpService[F] =
    placeOrderEndpoint(orderService)
}

object OrderEndpoints {
  def endpoints[F[_]: Sync](orderService: OrderService[F]): HttpService[F] = {
    new OrderEndpoints[F].endpoints(orderService)
  }
}

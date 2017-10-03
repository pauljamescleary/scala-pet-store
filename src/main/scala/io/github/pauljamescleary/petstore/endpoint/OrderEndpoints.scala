package io.github.pauljamescleary.petstore.endpoint

import cats.effect.IO
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import io.github.pauljamescleary.petstore.model.{Order, OrderStatus}
import io.github.pauljamescleary.petstore.service.OrderService
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl._

import scala.language.higherKinds

object OrderEndpoints {

  /* Need Joda DateTime Json Encoding */
  import JodaDateTime._

  /* Needed for service composition via |+| */
  import cats.implicits._

  /* We need to define an enum encoder and decoder since these do not come out of the box with generic derivation */
  implicit val statusDecoder = deriveEnumerationDecoder[OrderStatus]
  implicit val statusEncoder = deriveEnumerationEncoder[OrderStatus]

  def placeOrderEndpoint(orderService: OrderService[IO]): HttpService[IO] =
    HttpService[IO] {
      case req @ POST -> Root / "orders" => {
        for {
          order <- req.as(implicitly, jsonOf[IO, Order]) // <-- TODO: Make this cleaner in HTTP4S
          saved <- orderService.placeOrder(order)
          resp <- Ok(saved.asJson)
        } yield resp
      }
    }

  def endpoints(orderService: OrderService[IO]): HttpService[IO] =
    placeOrderEndpoint(orderService)
}

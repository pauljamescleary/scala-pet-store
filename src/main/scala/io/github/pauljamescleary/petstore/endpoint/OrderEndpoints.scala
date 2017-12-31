package io.github.pauljamescleary.petstore.endpoint

import cats.effect.Effect
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import io.circe.syntax._
import io.github.pauljamescleary.petstore.model.{Order, OrderStatus, User}
import io.github.pauljamescleary.petstore.service.OrderService
import io.github.pauljamescleary.petstore.validation.OrderNotFoundError
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.mac.imports.HMACSHA256

import scala.language.higherKinds

class OrderEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  /* Needed for service composition via |+| */
  import cats.implicits._

  /* We need to define an enum encoder and decoder since these do not come out of the box with generic derivation */
  implicit val statusDecoder: Decoder[OrderStatus] = deriveEnumerationDecoder
  implicit val statusEncoder: Encoder[OrderStatus] = deriveEnumerationEncoder

  /* Needed to decode entities */
  implicit val orderDecoder = jsonOf[F, Order]

  def placeOrderEndpoint(orderService: OrderService[F], auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[HMACSHA256, Long]]): HttpService[F] =
    auth {
      case req @ POST -> Root / "orders" asAuthed _ => {
        for {
          order <- req.request.as[Order]
          saved <- orderService.placeOrder(order)
          resp <- Ok(saved.asJson)
        } yield resp
      }
    }

  private def getOrderEndpoint(orderService: OrderService[F], auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[HMACSHA256, Long]]): HttpService[F] =
    auth {
      case GET -> Root / "orders" / LongVar(id) asAuthed _ =>
        orderService.get(id).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(OrderNotFoundError) => NotFound("The order was not found")
        }
    }

  private def deleteOrderEndpoint(orderService: OrderService[F], auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[HMACSHA256, Long]]): HttpService[F] =
    auth {
      case DELETE -> Root / "orders" / LongVar(id) asAuthed _ =>
        for {
          _ <- orderService.delete(id)
          resp <- Ok()
        } yield resp
    }

  def endpoints(orderService: OrderService[F], auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[HMACSHA256, Long]]): HttpService[F] =
    placeOrderEndpoint(orderService, auth) <+> getOrderEndpoint(orderService, auth) <+> deleteOrderEndpoint(orderService, auth)
}

object OrderEndpoints {
  def endpoints[F[_]: Effect](orderService: OrderService[F], auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[HMACSHA256, Long]]): HttpService[F] =
    new OrderEndpoints[F].endpoints(orderService, auth)
}

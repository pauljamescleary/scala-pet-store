package io.github.pauljamescleary.petstore.infrastructure.endpoint

import io.github.pauljamescleary.petstore.domain.orders._
import io.github.pauljamescleary.petstore.PetStoreArbitraries
import io.github.pauljamescleary.petstore.infrastructure.repository.inmemory.OrderRepositoryInMemoryInterpreter
import cats.effect._
//import io.circe.generic.auto._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.scalatest._
import org.scalatest.prop.PropertyChecks


class OrderEndpointsSpec
    extends FunSuite
    with Matchers
    with PropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  implicit val statusDec : EntityDecoder[IO, OrderStatus] = jsonOf
  implicit val statusEnc : EntityEncoder[IO, OrderStatus] = jsonEncoderOf

  implicit val orderEncoder : Encoder[Order] = deriveEncoder
  implicit val orderEnc : EntityEncoder[IO, Order] = jsonEncoderOf

  test("place order") {

    val orderService = OrderService(OrderRepositoryInMemoryInterpreter[IO]())
    val orderHttpService = OrderEndpoints.endpoints[IO](orderService).orNotFound

    forAll { (order: Order) =>
      (for {
        request <- POST(order, Uri.uri("/orders"))
        response <- orderHttpService.run(request)
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }

  }

}

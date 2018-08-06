package io.github.pauljamescleary.petstore.infrastructure.endpoint

import io.github.pauljamescleary.petstore.domain.orders._
import io.github.pauljamescleary.petstore.PetStoreArbitraries
import io.github.pauljamescleary.petstore.infrastructure.repository.inmemory.OrderRepositoryInMemoryInterpreter
import cats.effect._
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.syntax._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.scalatest._
import org.scalatest.prop.PropertyChecks


class OrderEndpointsSpec
    extends FunSuite
    with Matchers
    with PropertyChecks
    with PetStoreArbitraries
    with Http4sDsl[IO] {

  test("place order") {

    val orderService = OrderService(OrderRepositoryInMemoryInterpreter[IO]())
    val orderHttpService = OrderEndpoints.endpoints[IO](orderService)

    forAll { (order: Order) =>
      val placeOrderReq =
        Request[IO](Method.POST, Uri.uri("/orders")).withBody(order.asJson)

      (for {
        request <- placeOrderReq
        response <- orderHttpService
          .run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield {
        response.status shouldEqual Ok
      }).unsafeRunSync
    }

  }

}

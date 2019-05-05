package io.github.pauljamescleary.petstore
package infrastructure.endpoint

import domain.orders._
import infrastructure.repository.inmemory._
import cats.effect._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.mac.jca.HMACSHA256

class OrderEndpointsSpec
   extends FunSuite
   with Matchers
   with ScalaCheckPropertyChecks
   with PetStoreArbitraries
   with Http4sDsl[IO]
   with Http4sClientDsl[IO] {

 implicit val statusDec : EntityDecoder[IO, OrderStatus] = jsonOf
 implicit val statusEnc : EntityEncoder[IO, OrderStatus] = jsonEncoderOf

 implicit val orderEncoder : Encoder[Order] = deriveEncoder
 implicit val orderEnc : EntityEncoder[IO, Order] = jsonEncoderOf
 implicit val orderDecoder: Decoder[Order] = deriveDecoder
 implicit val orderDec: EntityDecoder[IO, Order] = jsonOf

 test("place and get order") {

   val userRepo = UserRepositoryInMemoryInterpreter[IO]()
   val auth = new AuthTest[IO](userRepo)
   val orderService = OrderService(OrderRepositoryInMemoryInterpreter[IO]())
   val orderHttpService = OrderEndpoints.endpoints[IO, HMACSHA256](orderService, auth.securedRqHandler).orNotFound

   forAll { (order: Order, user: AdminUser) =>
     (for {
       createRq <- POST(order, Uri.uri("/orders"))
       createRqAuth <- auth.embedToken(user.value, createRq)
       createResp <- orderHttpService.run(createRqAuth)
       orderResp <- createResp.as[Order]
       getOrderRq <- GET(Uri.unsafeFromString(s"/orders/${orderResp.id.get}"))
       getOrderRqAuth <- auth.embedToken(user.value, getOrderRq)
       getOrderResp <- orderHttpService.run(getOrderRqAuth)
       orderResp2 <- getOrderResp.as[Order]
     } yield {
       createResp.status shouldEqual Ok
       orderResp.petId shouldBe order.petId
       getOrderResp.status shouldEqual Ok
       orderResp2.userId should be ('defined)
     }).unsafeRunSync
   }
 }

  test("user roles") {

    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val auth = new AuthTest[IO](userRepo)
    val orderService = OrderService(OrderRepositoryInMemoryInterpreter[IO]())
    val orderHttpService = OrderEndpoints.endpoints[IO, HMACSHA256](orderService, auth.securedRqHandler).orNotFound

    forAll { user: CustomerUser =>
      (for {
        deleteRq <- DELETE(Uri.unsafeFromString(s"/orders/1"))
          .flatMap(auth.embedToken(user.value, _))
        deleteResp <- orderHttpService.run(deleteRq)
      } yield {
        deleteResp.status shouldEqual Unauthorized
      }).unsafeRunSync
    }

    forAll { user: AdminUser =>
      (for {
        deleteRq <- DELETE(Uri.unsafeFromString(s"/orders/1"))
          .flatMap(auth.embedToken(user.value, _))
        deleteResp <- orderHttpService.run(deleteRq)
      } yield {
        deleteResp.status shouldEqual Ok
      }).unsafeRunSync
    }
  }
}

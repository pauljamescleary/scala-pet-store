package io.github.pauljamescleary.petstore.infrastructure.endpoint

import io.github.pauljamescleary.petstore.domain.users._
import io.github.pauljamescleary.petstore.domain.orders._
import io.github.pauljamescleary.petstore.PetStoreArbitraries
import io.github.pauljamescleary.petstore.infrastructure.repository.inmemory._
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

 test("place order") {

   val userRepo = UserRepositoryInMemoryInterpreter[IO]()
   val auth = new AuthTest[IO](userRepo)
   val orderService = OrderService(OrderRepositoryInMemoryInterpreter[IO]())
   val orderHttpService = OrderEndpoints.endpoints[IO, HMACSHA256](orderService, auth.securedRqHandler).orNotFound

   forAll { (order: Order, user: User) =>
     (for {
       request <- POST(order, Uri.uri("/orders"))
       authRequest <- auth.embedToken(user, request)
       response <- orderHttpService.run(authRequest)
     } yield {
       response.status shouldEqual Ok
     }).unsafeRunSync
   }

 }

}

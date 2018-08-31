package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import org.scalatest._
import cats.effect.IO
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor

import PetStoreArbitraries.order

class OrderQueryTypeCheckSpec extends FunSuite with Matchers with IOChecker {
  import OrderSQL._

  override val transactor : Transactor[IO] = testTransactor

  test("Typecheck order queries") {
    check(delete(1L))
    check(select(1L))

    order.arbitrary.sample.map{ o =>
      check(insert(o))
    }
  }
}

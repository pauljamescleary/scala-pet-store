package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import org.scalatest._
import cats.effect.IO
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import PetStoreArbitraries.pet
import cats.data.NonEmptyList
import cats.syntax.applicative._


class PetQueryTypeCheckSpec extends FunSuite with Matchers with IOChecker {
  override val transactor : Transactor[IO] = testTransactor

  import PetQueries._

  test("Typecheck pet queries") {
    pet.arbitrary.sample.map { p =>
      check(selectByStatus(p.status.pure[NonEmptyList]))
    }

    check(selectTagLikeString("example".pure[NonEmptyList]))
    check(select(1L))
    check(selectAll)
    check(delete(1L))
    check(selectByNameAndCategory("name", "category"))
  }
}

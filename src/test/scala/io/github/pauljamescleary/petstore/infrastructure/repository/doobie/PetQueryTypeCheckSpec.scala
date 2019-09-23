package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.applicative._
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import org.scalatest.funsuite.AnyFunSuite
import PetStoreArbitraries.pet
import org.scalatest.matchers.should.Matchers

class PetQueryTypeCheckSpec extends AnyFunSuite with Matchers with IOChecker {
  override val transactor: Transactor[IO] = testTransactor

  import PetSQL._

  test("Typecheck pet queries") {
    pet.arbitrary.sample.map { p =>
      check(selectByStatus(p.status.pure[NonEmptyList]))
      check(insert(p))
      p.id.foreach(id => check(PetSQL.update(p, id)))
    }

    check(selectTagLikeString("example".pure[NonEmptyList]))
    check(select(1L))
    check(selectAll)
    check(delete(1L))
    check(selectByNameAndCategory("name", "category"))
  }
}

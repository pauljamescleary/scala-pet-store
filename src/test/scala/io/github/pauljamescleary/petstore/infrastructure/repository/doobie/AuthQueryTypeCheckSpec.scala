package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import org.scalatest._
import cats.effect.IO
import doobie.scalatest.IOChecker
import org.scalatest.Matchers
import tsec.mac.jca.HMACSHA256
import io.github.pauljamescleary.petstore.PetStoreArbitraries._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.authentication.AugmentedJWT
import tsec.common.SecureRandomId

class AuthQueryTypeCheckSpec 
  extends FunSuite 
  with Matchers 
  with ScalaCheckPropertyChecks
  with IOChecker {
    override def transactor: doobie.Transactor[IO] = testTransactor

    import AuthSQL._

    test("Typecheck auth queries") {
        forAll { jwt: AugmentedJWT[HMACSHA256, Long] =>
            check(insert(jwt))
        }
        forAll { jwt: AugmentedJWT[HMACSHA256, Long] =>
            check(update(jwt))
        }
        forAll { id: SecureRandomId =>
            check(select(id))
        }
        forAll { id: SecureRandomId =>
            check(delete(id))
        }
    }
}

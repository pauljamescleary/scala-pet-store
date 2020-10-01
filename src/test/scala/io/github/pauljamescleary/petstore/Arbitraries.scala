package io.github.pauljamescleary.petstore

import java.time.Instant
import cats.effect.IO
import domain.authentication.SignupRequest
import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary
import domain.orders._
import domain.orders.OrderStatus._
import domain.{orders, pets}
import domain.pets._
import domain.pets.PetStatus._
import domain.users.{Role, _}
import tsec.common.SecureRandomId
import tsec.jwt.JWTClaims
import tsec.authentication.AugmentedJWT
import tsec.jws.mac._
import tsec.mac.jca._

trait PetStoreArbitraries {
  val userNameLength = 16
  val userNameGen: Gen[String] = Gen.listOfN(userNameLength, Gen.alphaChar).map(_.mkString)

  implicit val instant = Arbitrary[Instant] {
    for {
      millis <- Gen.posNum[Long]
    } yield Instant.ofEpochMilli(millis)
  }

  implicit val orderStatus = Arbitrary[OrderStatus] {
    Gen.oneOf(Approved, Delivered, Placed)
  }

  def order(userId: Option[Long]): Arbitrary[Order] = Arbitrary[Order] {
    for {
      petId <- Gen.posNum[Long]
      shipDate <- Gen.option(instant.arbitrary)
      status <- arbitrary[OrderStatus]
      complete <- arbitrary[Boolean]
      id <- Gen.option(Gen.posNum[Long])
    } yield orders.Order(petId, shipDate, status, complete, id, userId)
  }

  implicit val orderNoUser: Arbitrary[Order] = order(None)

  implicit val petStatus = Arbitrary[PetStatus] {
    Gen.oneOf(Available, Pending, Adopted)
  }

  implicit val pet = Arbitrary[Pet] {
    for {
      name <- Gen.nonEmptyListOf(Gen.asciiPrintableChar).map(_.mkString)
      category <- arbitrary[String]
      bio <- arbitrary[String]
      status <- arbitrary[PetStatus]
      numTags <- Gen.choose(0, 10)
      tags <- Gen.listOfN(numTags, Gen.alphaStr).map(_.toSet)
      photoUrls <- Gen
        .listOfN(numTags, Gen.alphaStr)
        .map(_.map(x => s"http://${x}.com"))
        .map(_.toSet)
      id <- Gen.option(Gen.posNum[Long])
    } yield pets.Pet(name, category, bio, status, tags, photoUrls, id)
  }

  implicit val role = Arbitrary[Role](Gen.oneOf(Role.values.toIndexedSeq))

  implicit val user = Arbitrary[User] {
    for {
      userName <- userNameGen
      firstName <- arbitrary[String]
      lastName <- arbitrary[String]
      email <- arbitrary[String]
      password <- arbitrary[String]
      phone <- arbitrary[String]
      id <- Gen.option(Gen.posNum[Long])
      role <- arbitrary[Role]
    } yield User(userName, firstName, lastName, email, password, phone, id, role)
  }

  case class AdminUser(value: User)
  case class CustomerUser(value: User)

  implicit val adminUser: Arbitrary[AdminUser] = Arbitrary {
    user.arbitrary.map(user => AdminUser(user.copy(role = Role.Admin)))
  }

  implicit val customerUser: Arbitrary[CustomerUser] = Arbitrary {
    user.arbitrary.map(user => CustomerUser(user.copy(role = Role.Customer)))
  }

  implicit val userSignup = Arbitrary[SignupRequest] {
    for {
      userName <- userNameGen
      firstName <- arbitrary[String]
      lastName <- arbitrary[String]
      email <- arbitrary[String]
      password <- arbitrary[String]
      phone <- arbitrary[String]
      role <- arbitrary[Role]
    } yield SignupRequest(userName, firstName, lastName, email, password, phone, role)
  }

  implicit val secureRandomId = Arbitrary[SecureRandomId] {
    arbitrary[String].map(SecureRandomId.apply)
  }

  implicit val jwtMac: Arbitrary[JWTMac[HMACSHA256]] = Arbitrary {
    for {
      key <- Gen.const(HMACSHA256.unsafeGenerateKey)
      claims <- Gen.finiteDuration.map(exp =>
        JWTClaims.withDuration[IO](expiration = Some(exp)).unsafeRunSync(),
      )
    } yield JWTMacImpure
      .build[HMACSHA256](claims, key)
      .getOrElse(throw new Exception("Inconceivable"))
  }

  implicit def augmentedJWT[A, I](implicit
      arb1: Arbitrary[JWTMac[A]],
      arb2: Arbitrary[I],
  ): Arbitrary[AugmentedJWT[A, I]] =
    Arbitrary {
      for {
        id <- arbitrary[SecureRandomId]
        jwt <- arb1.arbitrary
        identity <- arb2.arbitrary
        expiry <- arbitrary[Instant]
        lastTouched <- Gen.option(arbitrary[Instant])
      } yield AugmentedJWT(id, jwt, identity, expiry, lastTouched)
    }
}

object PetStoreArbitraries extends PetStoreArbitraries

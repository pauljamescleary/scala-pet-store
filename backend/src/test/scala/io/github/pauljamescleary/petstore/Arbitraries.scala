package io.github.pauljamescleary.petstore

import java.time.Instant

import io.github.pauljamescleary.petstore.domain.authentication.SignupRequest
import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary
import io.github.pauljamescleary.petstore.domain.orders._
import io.github.pauljamescleary.petstore.domain.orders.OrderStatus._
import io.github.pauljamescleary.petstore.domain.{orders, pets}
import io.github.pauljamescleary.petstore.domain.pets._
import io.github.pauljamescleary.petstore.domain.pets.PetStatus._
import io.github.pauljamescleary.petstore.domain.users._


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

  implicit val order = Arbitrary[Order] {
    for {
      petId <- Gen.posNum[Long]
      shipDate <- Gen.option(instant.arbitrary)
      status <- arbitrary[OrderStatus]
      complete <- arbitrary[Boolean]
      id <- Gen.option(Gen.posNum[Long])
    } yield orders.Order(petId, shipDate, status, complete, id)
  }

  implicit val petStatus = Arbitrary[PetStatus] {
    Gen.oneOf(Available, Pending, Adopted)
  }

  implicit val pet = Arbitrary[Pet] {
    for {
      name <- arbitrary[String]
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

  implicit val user = Arbitrary[User] {
    for {
      userName <- userNameGen
      firstName <- arbitrary[String]
      lastName <- arbitrary[String]
      email <- arbitrary[String]
      password <- arbitrary[String]
      phone <- arbitrary[String]
      id <- Gen.option(Gen.posNum[Long])
    } yield User(userName, firstName, lastName, email, password, phone, id)
  }

  implicit val userSignup = Arbitrary[SignupRequest] {
    for {
      userName <- userNameGen
      firstName <- arbitrary[String]
      lastName <- arbitrary[String]
      email <- arbitrary[String]
      password <- arbitrary[String]
      phone <- arbitrary[String]
    } yield SignupRequest(userName, firstName, lastName, email, password, phone)
  }
}

object PetStoreArbitraries extends PetStoreArbitraries

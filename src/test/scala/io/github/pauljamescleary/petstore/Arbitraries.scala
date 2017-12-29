package io.github.pauljamescleary.petstore

import java.time.Instant

import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary
import io.github.pauljamescleary.petstore.domain.orders._
import io.github.pauljamescleary.petstore.domain.{orders, pets}
import io.github.pauljamescleary.petstore.domain.pets._

trait PetStoreArbitraries {

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

}

object PetStoreArbitraries extends PetStoreArbitraries

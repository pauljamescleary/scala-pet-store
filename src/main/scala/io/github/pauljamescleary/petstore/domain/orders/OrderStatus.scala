package io.github.pauljamescleary.petstore.domain.orders
import enumeratum._

sealed trait OrderStatus extends EnumEntry

case object OrderStatus extends Enum[OrderStatus] with CirceEnum[OrderStatus] {
  case object Approved extends OrderStatus
  case object Delivered extends OrderStatus
  case object Placed extends OrderStatus

  val values = findValues
}

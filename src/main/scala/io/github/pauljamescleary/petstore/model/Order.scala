package io.github.pauljamescleary.petstore.model

import org.joda.time.DateTime

case class Order(
    petId: Long,
    shipDate: Option[DateTime] = None,
    status: OrderStatus = OrderStatus.Placed,
    complete: Boolean = false,
    id: Option[Long] = None
)

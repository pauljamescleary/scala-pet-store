package io.github.pauljamescleary.petstore.domain.orders

import cats.effect.IO

trait OrderRepositoryAlgebra {
  def create(order: Order): IO[Order]

  def get(orderId: Long): IO[Option[Order]]

  def delete(orderId: Long): IO[Option[Order]]
}

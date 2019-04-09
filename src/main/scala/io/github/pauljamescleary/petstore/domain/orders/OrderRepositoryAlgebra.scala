package io.github.pauljamescleary.petstore.domain.orders

import cats.effect.Bracket

import scala.language.higherKinds

trait OrderRepositoryAlgebra[F[_]] {
  type B = Bracket[F, Throwable]

  def create(order: Order)(implicit b: B): F[Order]

  def get(orderId: Long)(implicit b: B): F[Option[Order]]

  def delete(orderId: Long)(implicit b: B): F[Option[Order]]
}

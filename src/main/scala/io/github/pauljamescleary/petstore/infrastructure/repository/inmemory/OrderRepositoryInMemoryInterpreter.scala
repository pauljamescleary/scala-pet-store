package io.github.pauljamescleary.petstore.infrastructure.repository.inmemory

import scala.collection.concurrent.TrieMap
import scala.util.Random

import cats._
import cats.implicits._
import io.github.pauljamescleary.petstore.domain.orders.{Order, OrderRepositoryAlgebra}

class OrderRepositoryInMemoryInterpreter[F[_]: Applicative] extends OrderRepositoryAlgebra[F] {

  private val cache = new TrieMap[Long, Order]

  private val random = new Random

  def put(order: Order): F[Order] = {
    val toSave =
      if (order.id.isDefined) order
      else order.copy(id = Some(random.nextLong))

    toSave.id.foreach { cache.put(_, toSave) }
    toSave.pure[F]
  }

  def get(orderId: Long): F[Option[Order]] =
    cache.get(orderId).pure[F]

  def delete(orderId: Long): F[Option[Order]] =
    cache.remove(orderId).pure[F]

}

object OrderRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new OrderRepositoryInMemoryInterpreter[F]()
}

package io.github.pauljamescleary.petstore
package infrastructure.repository.inmemory

import scala.collection.concurrent.TrieMap
import scala.util.Random

import cats._
import cats.syntax.all._
import domain.orders.{Order, OrderRepositoryAlgebra}

class OrderRepositoryInMemoryInterpreter[F[_]: Applicative] extends OrderRepositoryAlgebra[F] {
  private val cache = new TrieMap[Long, Order]

  private val random = new Random

  def create(order: Order): F[Order] = {
    val toSave = order.copy(id = order.id.orElse(random.nextLong().some))
    toSave.id.foreach(cache.put(_, toSave))
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

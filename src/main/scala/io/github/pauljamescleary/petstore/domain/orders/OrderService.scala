package io.github.pauljamescleary.petstore.domain
package orders

import cats.data.EitherT
import cats.effect.IO

class OrderService(orderRepo: OrderRepositoryAlgebra) {
  def placeOrder(order: Order): IO[Order] =
    orderRepo.create(order)

  def get(id: Long): IO[Either[OrderNotFoundError.type, Order]] =
    EitherT.fromOptionF(orderRepo.get(id), OrderNotFoundError).value

  def delete(id: Long): IO[Unit] =
    orderRepo.delete(id).as(())
}

object OrderService {
  def apply(orderRepo: OrderRepositoryAlgebra): OrderService =
    new OrderService(orderRepo)
}

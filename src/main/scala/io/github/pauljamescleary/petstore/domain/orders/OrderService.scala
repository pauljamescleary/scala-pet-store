package io.github.pauljamescleary.petstore.domain
package orders

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import io.github.pauljamescleary.petstore.domain.orders.OrderRequest._

class OrderService[F[_]: Monad](orderRepo: OrderRepositoryAlgebra[F], queue: OrderQueueAlgebra[F]) {

  def placeOrder(order: Order): F[Order] =
    queue.send(Submit(order)).as(order)

  def get(id: Long): EitherT[F, OrderNotFoundError.type, Order] =
    EitherT.fromOptionF(orderRepo.get(id), OrderNotFoundError)

  def cancel(id: Long): EitherT[F, OrderNotFoundError.type, Order] =
    get(id).semiflatMap(order => queue.send(Cancel(order)).as(order))
}

object OrderService {
  def apply[F[_]: Monad](
      orderRepo: OrderRepositoryAlgebra[F],
      queue: OrderQueueAlgebra[F],
  ): OrderService[F] =
    new OrderService(orderRepo, queue)
}

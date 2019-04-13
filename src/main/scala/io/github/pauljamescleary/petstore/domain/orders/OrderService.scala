package io.github.pauljamescleary.petstore.domain.orders

import cats.data.EitherT
import cats.effect.Bracket
import io.github.pauljamescleary.petstore.domain.OrderNotFoundError

class OrderService[F[_]](orderRepo: OrderRepositoryAlgebra[F])(implicit B: Bracket[F, Throwable]) {
  import cats.syntax.all._

  def placeOrder(order: Order): F[Order] = orderRepo.create(order)

  def get(id: Long): EitherT[F, OrderNotFoundError.type, Order] =
    EitherT.fromOptionF(orderRepo.get(id), OrderNotFoundError)


  def delete(id: Long): F[Unit] =
    orderRepo.delete(id).as(())
}

object OrderService {
  def apply[F[_]](orderRepo: OrderRepositoryAlgebra[F])(
  	implicit B: Bracket[F, Throwable]
  ): OrderService[F] =
    new OrderService(orderRepo)
}

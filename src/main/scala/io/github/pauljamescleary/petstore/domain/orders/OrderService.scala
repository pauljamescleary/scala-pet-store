package io.github.pauljamescleary.petstore.domain.orders

import cats.data.EitherT
import cats.effect.Bracket
import io.github.pauljamescleary.petstore.domain.OrderNotFoundError

class OrderService[F[_]](orderRepo: OrderRepositoryAlgebra[F]) {
  import cats.syntax.all._

  type B = Bracket[F, Throwable]

  def placeOrder(order: Order)(implicit b: B): F[Order] = orderRepo.create(order)

  def get(id: Long)(implicit b: B): EitherT[F, OrderNotFoundError.type, Order] =
    EitherT.fromOptionF(orderRepo.get(id), OrderNotFoundError)


  def delete(id: Long)(implicit b: B): F[Unit] =
    orderRepo.delete(id).as(())
}

object OrderService {
  def apply[F[_]](orderRepo: OrderRepositoryAlgebra[F]): OrderService[F] =
    new OrderService(orderRepo)
}

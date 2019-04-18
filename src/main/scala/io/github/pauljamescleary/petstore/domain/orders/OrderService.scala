package io.github.pauljamescleary.petstore.domain.orders

import scala.language.higherKinds

import cats.Monad
import cats.data.EitherT
import io.github.pauljamescleary.petstore.domain.OrderNotFoundError

class OrderService[F[_]](orderRepo: OrderRepositoryAlgebra[F]) {
  import cats.syntax.all._

  def placeOrder(order: Order): F[Order] = orderRepo.
    create(order)

  def get(id: Long)(implicit M: Monad[F]): EitherT[F, OrderNotFoundError.type, Order] =
    EitherT.fromOptionF(orderRepo.get(id), OrderNotFoundError)


  def delete(id: Long)(implicit M: Monad[F]): F[Unit] =
    orderRepo.delete(id).as(())
}

object OrderService {
  def apply[F[_]](orderRepo: OrderRepositoryAlgebra[F]): OrderService[F] =
    new OrderService(orderRepo)
}

package io.github.pauljamescleary.petstore.domain
package orders

import scala.language.higherKinds

import cats.Functor
import cats.data.EitherT

class OrderService[F[_]](orderRepo: OrderRepositoryAlgebra[F]) {
  import cats.implicits._

  def placeOrder(order: Order): F[Order] =
    orderRepo.create(order)

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, OrderNotFoundError.type, Order] =
    EitherT.fromOptionF(orderRepo.get(id), OrderNotFoundError)

  def delete(id: Long)(implicit F: Functor[F]): F[Unit] =
    orderRepo.delete(id).as(())
}

object OrderService {
  def apply[F[_]](orderRepo: OrderRepositoryAlgebra[F]): OrderService[F] =
    new OrderService(orderRepo)
}

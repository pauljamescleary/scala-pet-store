package io.github.pauljamescleary.petstore.service

import cats.Monad
import cats.data.EitherT
import io.github.pauljamescleary.petstore.model.Order
import io.github.pauljamescleary.petstore.repository.OrderRepositoryAlgebra
import io.github.pauljamescleary.petstore.validation.OrderNotFoundError

import scala.language.higherKinds

class OrderService[F[_]](orderRepo: OrderRepositoryAlgebra[F]) {
  import cats.syntax.all._

  def placeOrder(order: Order): F[Order] = orderRepo.put(order)

  def get(id: Long)(implicit M: Monad[F]): EitherT[F, OrderNotFoundError, Order] =
    EitherT {
      orderRepo.get(id).map {
        case None => Left(OrderNotFoundError())
        case Some(order) => Right(order)
      }
    }
}

object OrderService {
  def apply[F[_]](orderRepo: OrderRepositoryAlgebra[F]): OrderService[F] =
    new OrderService(orderRepo)
}

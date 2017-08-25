package io.github.pauljamescleary.petstore.service

import cats.Monad
import io.github.pauljamescleary.petstore.model.Order
import io.github.pauljamescleary.petstore.repository.OrderRepositoryAlgebra

import scala.language.higherKinds

class OrderService[F[_] : Monad](implicit val orderRepo: OrderRepositoryAlgebra[F]) {

  def placeOrder(order: Order): F[Order] = orderRepo.put(order)
}

package io.github.pauljamescleary.petstore.domain
package orders

import cats.Applicative
import cats.implicits._
import cats.data.EitherT
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.Applicative

class OrderService[F[_]: Logger: Applicative](orderRepo: OrderRepositoryAlgebra[F]) {
  def placeOrder(order: Order): F[Order] =
    orderRepo.create(order)

  def get(id: Long): EitherT[F, OrderNotFoundError.type, Order] =
    EitherT.fromOptionF(
      Logger[F].info(s"Fetching order with id $id") *> orderRepo.get(id),
      OrderNotFoundError,
    )

  def delete(id: Long): F[Unit] =
    Logger[F].info(s"Deleting order with id $id") *> orderRepo.delete(id).as(())
}

object OrderService {
  def apply[F[_]: Sync](orderRepo: OrderRepositoryAlgebra[F]): Resource[F, OrderService[F]] =
    Resource.eval(Slf4jLogger.create[F]).map { implicit logger: Logger[F] =>
      new OrderService(orderRepo)
    }
}

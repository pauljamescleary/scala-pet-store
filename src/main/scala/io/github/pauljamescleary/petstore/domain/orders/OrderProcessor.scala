package io.github.pauljamescleary.petstore.domain.orders

import cats.Monad
import cats.data.OptionT
import cats.effect.Timer
import cats.implicits._
import fs2.{Pipe, Stream}
import io.github.pauljamescleary.petstore.domain.orders.OrderRequest.{Cancel, Submit}

import scala.concurrent.duration._

object OrderProcessor {

  def poll[F[_]: Monad: Timer](
      interval: FiniteDuration,
      queue: OrderQueueAlgebra[F],
  ): Stream[F, OrderRequest] =
    for {
      _ <- Stream.fixedDelay(interval).covary[F]
      requestStream <- Stream.evalSeq(queue.receive().map(_.toSeq))
    } yield requestStream

  def handleOrder[F[_]: Monad](
      repo: OrderRepositoryAlgebra[F],
  ): Pipe[F, OrderRequest, OrderRequest] =
    _.evalMap { req =>
      req match {
        case Submit(order, _) =>
          repo
            .create(order)
            .as(req)
        case Cancel(order, _) =>
          OptionT
            .fromOption[F](order.id)
            .semiflatMap(repo.delete)
            .value
            .as(req)
      }
    }

  def cleanupMessage[F[_]: Monad](queue: OrderQueueAlgebra[F]): Pipe[F, OrderRequest, Unit] =
    _.evalMap(req => queue.delete(req.id).void)

  def flow[F[_]: Monad: Timer](
      repo: OrderRepositoryAlgebra[F],
      queue: OrderQueueAlgebra[F],
  ): Stream[F, Unit] =
    poll(5.seconds, queue)
      .through(handleOrder(repo))
      .through(cleanupMessage(queue))
}

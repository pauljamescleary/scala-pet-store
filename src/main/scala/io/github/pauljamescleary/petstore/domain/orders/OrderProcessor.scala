package io.github.pauljamescleary.petstore.domain.orders

import cats.{Functor, Monad}
import cats.data.OptionT
import cats.effect.Timer
import cats.implicits._
import fs2.{Pipe, Stream}
import io.github.pauljamescleary.petstore.domain.orders.OrderRequest.{Cancel, Submit}

import scala.concurrent.duration._

object OrderProcessor {

  /* Polls the message queue on an interval, generating a stream of orders only if one is present */
  def poll[F[_]: Functor: Timer](
      interval: FiniteDuration,
      queue: OrderQueueAlgebra[F],
  ): Stream[F, OrderRequest] =
    for {
      _ <- Stream.fixedDelay(interval).covary[F].map { _ => println("POLLING!") }
      requestStream <- Stream.evalSeq(queue.receive().map(_.toSeq))
    } yield requestStream

  /* Does something interesting with the order, save it or delete it, exciting amirite!? */
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

  /* Removes the message from the queue if processing is successful */
  def cleanupMessage[F[_]: Functor](queue: OrderQueueAlgebra[F]): Pipe[F, OrderRequest, Unit] =
    _.evalMap(req => queue.delete(req.id).void)

  /* Creates a stream that can be started in the background to poll for messages */
  def flow[F[_]: Monad: Timer](
      repo: OrderRepositoryAlgebra[F],
      queue: OrderQueueAlgebra[F],
  ): Stream[F, Unit] =
    poll(5.seconds, queue)
      .through(handleOrder(repo))
      .through(cleanupMessage(queue))
}

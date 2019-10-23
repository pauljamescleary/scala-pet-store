package io.github.pauljamescleary.petstore.infrastructure.queue.inmemory

import java.util.UUID

import cats.Applicative
import cats.data.OptionT
import cats.implicits._
import io.github.pauljamescleary.petstore.domain.orders.{OrderQueueAlgebra, OrderRequest}
import io.github.pauljamescleary.petstore.infrastructure.queue.inmemory.OrderQueueInMemoryInterpreter.QueueEntry

import scala.collection.concurrent.TrieMap

class OrderQueueInMemoryInterpreter[F[_]: Applicative] extends OrderQueueAlgebra[F] {
  private val q = new TrieMap[UUID, QueueEntry]

  def send[A <: OrderRequest](cmd: A): F[Unit] =
    OptionT.fromOption[F](q.put(cmd.id, QueueEntry(cmd))).value.void

  def receive(): F[Option[OrderRequest]] =
    OptionT
      .fromOption[F] {
        for {
          avail <- q.values.find(!_.inFlight)
          claimed = avail.copy(inFlight = true)
          _ <- q.put(claimed.req.id, claimed)
        } yield claimed.req
      }
      .value

  def delete(id: UUID): F[Option[OrderRequest]] =
    OptionT.fromOption[F](q.remove(id).map(_.req)).value
}
object OrderQueueInMemoryInterpreter {
  final case class QueueEntry(req: OrderRequest, inFlight: Boolean = false)

  def apply[F[_]: Applicative](): OrderQueueAlgebra[F] = new OrderQueueInMemoryInterpreter[F]
}

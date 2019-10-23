package io.github.pauljamescleary.petstore.domain.orders

import java.util.UUID

sealed trait OrderRequest {
  def id: OrderRequest.RequestId
}
object OrderRequest {
  type RequestId = UUID
  final case class Submit(order: Order, id: RequestId = UUID.randomUUID()) extends OrderRequest
  final case class Cancel(order: Order, id: RequestId = UUID.randomUUID()) extends OrderRequest
}

trait OrderQueueAlgebra[F[_]] {
  def send[A <: OrderRequest](cmd: A): F[Unit]
  def receive(): F[Option[OrderRequest]]
  def delete(id: OrderRequest.RequestId): F[Option[OrderRequest]]
}

package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import java.time.Instant

import cats._
import cats.data.OptionT
import cats.implicits._
import doobie._
import doobie.implicits._
import domain.orders.{OrderRepositoryAlgebra, OrderStatus, Order}

private object OrderSQL {
  /* We require type StatusMeta to handle our ADT Status */
  implicit val StatusMeta: Meta[OrderStatus] =
    Meta[String].imap(OrderStatus.withName)(_.entryName)

  /* We require conversion for date time */
  implicit val DateTimeMeta: Meta[Instant] =
    Meta[java.sql.Timestamp].imap(_.toInstant)(java.sql.Timestamp.from _)

  def select(orderId: Long): Query0[Order] = sql"""
    SELECT PET_ID, SHIP_DATE, STATUS, COMPLETE, ID
    FROM ORDERS
    WHERE ID = $orderId
  """.query[Order]

  def insert(order : Order) : Update0 = sql"""
    INSERT INTO ORDERS (PET_ID, SHIP_DATE, STATUS, COMPLETE)
    VALUES (${order.petId}, ${order.shipDate}, ${order.status}, ${order.complete})
  """.update

  def delete(orderId : Long) : Update0 = sql"""
    DELETE FROM ORDERS
    WHERE ID = $orderId
  """.update
}

class DoobieOrderRepositoryInterpreter[F[_]: Monad](val xa: Transactor[F])
    extends OrderRepositoryAlgebra[F] {
  import OrderSQL._

  def create(order: Order): F[Order] =
    insert(order).withUniqueGeneratedKeys[Long]("ID").map(id => order.copy(id = id.some)).transact(xa)

  def get(orderId: Long): F[Option[Order]] = OrderSQL.select(orderId).option.transact(xa)

  def delete(orderId: Long): F[Option[Order]] =
    OptionT(get(orderId)).semiflatMap(order =>
      OrderSQL.delete(orderId).run.transact(xa).as(order)
    ).value
}

object DoobieOrderRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieOrderRepositoryInterpreter[F] =
    new DoobieOrderRepositoryInterpreter(xa)
}

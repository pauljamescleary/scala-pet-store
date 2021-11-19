package io.github.pauljamescleary.petstore
package infrastructure.repository.doobie

import cats.data.OptionT
import cats.effect.{Bracket, IO}
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.implicits.legacy.instant._
import domain.orders.{Order, OrderRepositoryAlgebra, OrderStatus}

private object OrderSQL {
  /* We require type StatusMeta to handle our ADT Status */
  implicit val StatusMeta: Meta[OrderStatus] =
    Meta[String].imap(OrderStatus.withName)(_.entryName)

  def select(orderId: Long): Query0[Order] = sql"""
    SELECT PET_ID, SHIP_DATE, STATUS, COMPLETE, ID, USER_ID
    FROM ORDERS
    WHERE ID = $orderId
  """.query[Order]

  def insert(order: Order): Update0 = sql"""
    INSERT INTO ORDERS (PET_ID, SHIP_DATE, STATUS, COMPLETE, USER_ID)
    VALUES (${order.petId}, ${order.shipDate}, ${order.status}, ${order.complete}, ${order.userId.get})
  """.update

  def delete(orderId: Long): Update0 = sql"""
    DELETE FROM ORDERS
    WHERE ID = $orderId
  """.update
}

class DoobieOrderRepositoryInterpreter(val xa: Transactor[IO])
    extends OrderRepositoryAlgebra {
  import OrderSQL._

  def create(order: Order): IO[Order] =
    insert(order)
      .withUniqueGeneratedKeys[Long]("ID")
      .map(id => order.copy(id = id.some))
      .transact(xa)

  def get(orderId: Long): IO[Option[Order]] =
    OrderSQL.select(orderId).option.transact(xa)

  def delete(orderId: Long): IO[Option[Order]] =
    OptionT(get(orderId))
      .semiflatMap(order => OrderSQL.delete(orderId).run.transact(xa).as(order))
      .value
}

object DoobieOrderRepositoryInterpreter {
  def apply(
      xa: Transactor[IO],
  ): DoobieOrderRepositoryInterpreter =
    new DoobieOrderRepositoryInterpreter(xa)
}

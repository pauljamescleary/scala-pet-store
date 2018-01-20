package io.github.pauljamescleary.petstore.infrastructure.repository.doobie

import java.time.Instant

import cats._
import cats.implicits._
import doobie._
import doobie.implicits._
import io.github.pauljamescleary.petstore.domain.orders
import io.github.pauljamescleary.petstore.domain.orders.{OrderRepositoryAlgebra, OrderStatus}

private object OrderQueries {
  /* We require type StatusMeta to handle our ADT Status */
  private implicit val StatusMeta: Meta[OrderStatus] =
    Meta[String].xmap(OrderStatus.apply, OrderStatus.nameOf)

  /* We require conversion for date time */
  private implicit val DateTimeMeta: Meta[Instant] =
    Meta[java.sql.Timestamp].xmap(
      ts => ts.toInstant,
      dt => java.sql.Timestamp.from(dt)
    )

  def select(orderId: Long): Query0[orders.Order] = sql"""
    SELECT PET_ID, SHIP_DATE, STATUS, COMPLETE, ID
    FROM ORDERS
    WHERE ID = $orderId
  """.query[orders.Order]

  def update(order : orders.Order) : Update0 = sql"""
    REPLACE INTO ORDERS (PET_ID, SHIP_DATE, STATUS, COMPLETE)
    VALUES (${order.petId}, ${order.shipDate}, ${order.status}, ${order.complete})
  """.update

  def delete(orderId : Long) : Update0 = sql"""
    DELETE FROM ORDERS
    WHERE ID = $orderId
  """.update
}

class DoobieOrderRepositoryInterpreter[F[_]: Monad](val xa: Transactor[F])
    extends OrderRepositoryAlgebra[F] {

  def put(order: orders.Order): F[orders.Order] = {
    val insert: ConnectionIO[orders.Order] =
      for {
        id <- OrderQueries.update(order).withUniqueGeneratedKeys[Long]("ID")
      } yield order.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(orderId: Long): F[Option[orders.Order]] = OrderQueries.select(orderId).option.transact(xa)

  def delete(orderId: Long): F[Option[orders.Order]] =
    get(orderId).flatMap {
      case Some(order) => OrderQueries.delete(orderId).run.transact(xa).map(_ => Some(order))
      case None => none[orders.Order].pure[F]
    }
}

object DoobieOrderRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieOrderRepositoryInterpreter[F] =
    new DoobieOrderRepositoryInterpreter(xa)
}

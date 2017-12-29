package io.github.pauljamescleary.petstore.infrastructure.repository.doobie

import java.time.Instant

import cats._
import cats.implicits._
import doobie._
import doobie.implicits._
import io.github.pauljamescleary.petstore.domain.orders
import io.github.pauljamescleary.petstore.domain.orders.{OrderRepositoryAlgebra, OrderStatus}

class DoobieOrderRepositoryInterpreter[F[_]: Monad](val xa: Transactor[F])
    extends OrderRepositoryAlgebra[F] {

  /* We require type StatusMeta to handle our ADT Status */
  private implicit val StatusMeta: Meta[OrderStatus] =
    Meta[String].xmap(OrderStatus.apply, OrderStatus.nameOf)

  /* We require conversion for date time */
  private implicit val DateTimeMeta: Meta[Instant] =
    Meta[java.sql.Timestamp].xmap(
      ts => ts.toInstant,
      dt => java.sql.Timestamp.from(dt)
    )

  def put(order: orders.Order): F[orders.Order] = {
    val insert: ConnectionIO[orders.Order] =
      for {
        id <- sql"REPLACE INTO ORDERS (PET_ID, SHIP_DATE, STATUS, COMPLETE) values (${order.petId}, ${order.shipDate}, ${order.status}, ${order.complete})".update
          .withUniqueGeneratedKeys[Long]("ID")
      } yield order.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(orderId: Long): F[Option[orders.Order]] =
    sql"""
      SELECT PET_ID, SHIP_DATE, STATUS, COMPLETE, ID
        FROM ORDERS
       WHERE ID = $orderId
     """.query[orders.Order].option.transact(xa)

  def delete(orderId: Long): F[Option[orders.Order]] =
    get(orderId).flatMap {
      case Some(order) =>
        sql"DELETE FROM ORDERS WHERE ID = $orderId".update.run
          .transact(xa)
          .map(_ => Some(order))
      case None =>
        none[orders.Order].pure[F]
    }
}

object DoobieOrderRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieOrderRepositoryInterpreter[F] =
    new DoobieOrderRepositoryInterpreter(xa)
}

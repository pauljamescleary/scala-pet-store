package io.github.pauljamescleary.petstore.repository

import doobie._
import doobie.implicits._
import cats._
import cats.implicits._
import io.github.pauljamescleary.petstore.model.{Order, OrderStatus}
import org.joda.time.DateTime

class DoobieOrderRepositoryInterpreter[F[_]: Monad](val xa: Transactor[F])
    extends OrderRepositoryAlgebra[F] {

  // This will clear the database.  Note, this would typically be done via something like FLYWAY (TODO)
  private val dropOrdersTable = sql"""
    DROP TABLE IF EXISTS ORDERS
  """.update.run.transact(xa)

  // The tags column is controversial, could be a lookup table.  For our purposes, indexing on tags to allow searching is fine
  private val createOrdersTable = sql"""
    CREATE TABLE ORDERS (
      ID   SERIAL,
      PET_ID INT8 NOT NULL,
      SHIP_DATE TIMESTAMP NULL,
      STATUS VARCHAR NOT NULL,
      COMPLETE BOOLEAN NOT NULL
    )
  """.update.run.transact(xa)

  /* We require type StatusMeta to handle our ADT Status */
  private implicit val StatusMeta: Meta[OrderStatus] = Meta[String].xmap(
    s => OrderStatus(s) match {
           case Left(msg) =>
             //actually, you should not throw but find what is the correct business logic
             //to deal with such an incorrect data. Most likelly the DB was corrupted,
             //perhaps some app upgrade not correctly done. So, assign "bad order, human check needed status" ?)
             //at least it can be caught with attempt in doobie or higher in the IO stack
             throw new IllegalArgumentException(s"Invalid data found in database: $msg")
           case Right(x)  => x
         },
    OrderStatus.nameOf
  )

  /* We require conversion for date time */
  private implicit val DateTimeMeta: Meta[DateTime] =
    Meta[java.sql.Timestamp].xmap(
      ts => new DateTime(ts.getTime),
      dt => new java.sql.Timestamp(dt.getMillis)
    )

  def migrate: F[Int] =
    dropOrdersTable >> createOrdersTable

  def put(order: Order): F[Order] = {
    val insert: ConnectionIO[Order] =
      for {
        id <- sql"REPLACE INTO ORDERS (PET_ID, SHIP_DATE, STATUS, COMPLETE) values (${order.petId}, ${order.shipDate}, ${order.status}, ${order.complete})".update
          .withUniqueGeneratedKeys[Long]("ID")
      } yield order.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(orderId: Long): F[Option[Order]] =
    sql"""
      SELECT PET_ID, SHIP_DATE, STATUS, COMPLETE
        FROM ORDERS
       WHERE ID = $orderId
     """.query[Order].option.transact(xa)

  def delete(orderId: Long): F[Option[Order]] =
    get(orderId).flatMap {
      case Some(order) =>
        sql"DELETE FROM ORDERS WHERE ID = $orderId".update.run
          .transact(xa)
          .map(_ => Some(order))
      case None =>
        none[Order].pure[F]
    }
}

object DoobieOrderRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieOrderRepositoryInterpreter[F] =
    new DoobieOrderRepositoryInterpreter(xa)
}

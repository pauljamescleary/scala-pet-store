package io.github.pauljamescleary.petstore.repository

import doobie._
import doobie.implicits._
import cats.syntax.all._
import cats.effect.IO
import cats.effect.implicits._
import cats.implicits._
import doobie.h2.H2Transactor
import io.github.pauljamescleary.petstore.model.{Order, OrderStatus}
import org.joda.time.DateTime

class DoobieOrderRepositoryInterpreter(val xa: Transactor[IO]) extends OrderRepositoryAlgebra[IO] {

  // This will clear the database on start.  Note, this would typically be done via something like FLYWAY (TODO)
  sql"""
    DROP TABLE IF EXISTS ORDERS
  """.update.run.transact(xa).unsafeRunSync()

  // The tags column is controversial, could be a lookup table.  For our purposes, indexing on tags to allow searching is fine
  sql"""
    CREATE TABLE ORDERS (
      ID   SERIAL,
      PET_ID INT8 NOT NULL,
      SHIP_DATE TIMESTAMP NULL,
      STATUS VARCHAR NOT NULL,
      COMPLETE BOOLEAN NOT NULL
    )
  """.update.run.transact(xa).unsafeRunSync()

  /* We require type StatusMeta to handle our ADT Status */
  private implicit val StatusMeta: Meta[OrderStatus] = Meta[String].xmap(OrderStatus.apply, OrderStatus.nameOf)

  /* We require conversion for date time */
  private implicit val DateTimeMeta: Meta[DateTime] = Meta[java.sql.Timestamp].xmap(
    ts => new DateTime(ts.getTime),
    dt => new java.sql.Timestamp(dt.getMillis)
  )

  def put(order: Order): IO[Order] = {
    val insert: ConnectionIO[Order] =
      for {
        id <- sql"REPLACE INTO ORDERS (PET_ID, SHIP_DATE, STATUS, COMPLETE) values (${order.petId}, ${order.shipDate}, ${order.status}, ${order.complete})".update.withUniqueGeneratedKeys[Long]("ID")
      } yield order.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(orderId: Long): IO[Option[Order]] = {
    sql"""
      SELECT PET_ID, SHIP_DATE, STATUS, COMPLETE
        FROM ORDERS
       WHERE ID = $orderId
     """.query[Order].option.transact(xa)
  }

  def delete(orderId: Long): IO[Option[Order]] = {
    get(orderId).flatMap {
      case Some(order) =>
        sql"DELETE FROM ORDERS WHERE ID = $orderId".update.run.transact(xa).map(_ => Some(order))
      case None =>
        IO.pure(None)
    }
  }
}

object DoobieOrderRepositoryInterpreter {
  def apply(): DoobieOrderRepositoryInterpreter = {
    val xa = H2Transactor[IO]("jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "").unsafeRunSync()
    new DoobieOrderRepositoryInterpreter(xa)
  }
}

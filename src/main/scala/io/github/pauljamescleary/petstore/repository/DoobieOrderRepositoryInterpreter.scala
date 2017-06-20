package io.github.pauljamescleary.petstore.repository

import doobie.imports._
import cats._
import cats.data._
import cats.implicits._
import fs2.interop.cats._
import shapeless._
import shapeless.record.Record
import fs2.Task
import io.github.pauljamescleary.petstore.model.{Order, OrderStatus}
import org.joda.time.DateTime

class DoobieOrderRepositoryInterpreter(val xa: Transactor[Task]) extends OrderRepositoryAlgebra[Task] {

  // This will clear the database on start.  Note, this would typically be done via something like FLYWAY (TODO)
  sql"""
    DROP TABLE IF EXISTS ORDERS
  """.update.run.transact(xa).unsafeRun

  // The tags column is controversial, could be a lookup table.  For our purposes, indexing on tags to allow searching is fine
  sql"""
    CREATE TABLE ORDERS (
      ID   SERIAL,
      PET_ID INT8 NOT NULL,
      SHIP_DATE TIMESTAMP NULL,
      STATUS VARCHAR NOT NULL,
      COMPLETE BOOLEAN NOT NULL
    )
  """.update.run.transact(xa).unsafeRun

  /* We require type StatusMeta to handle our ADT Status */
  private implicit val StatusMeta: Meta[OrderStatus] = Meta[String].nxmap(OrderStatus.apply, OrderStatus.nameOf)

  /* We require conversion for date time */
  private implicit val DateTimeMeta: Meta[DateTime] = Meta[java.sql.Timestamp].nxmap(
    ts => new DateTime(ts.getTime),
    dt => new java.sql.Timestamp(dt.getMillis)
  )

  def put(order: Order): Task[Order] = {
    val insert: ConnectionIO[Order] =
      for {
        id <- sql"REPLACE INTO ORDERS (PET_ID, SHIP_DATE, STATUS, COMPLETE) values (${order.petId}, ${order.shipDate}, ${order.status}, ${order.complete})".update.withUniqueGeneratedKeys[Long]("ID")
      } yield order.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(orderId: Long): Task[Option[Order]] = {
    sql"""
      SELECT PET_ID, SHIP_DATE, STATUS, COMPLETE
        FROM ORDERS
       WHERE ID = $orderId
     """.query[Order].option.transact(xa)
  }

  def delete(orderId: Long): Task[Option[Order]] = {
    get(orderId).flatMap {
      case Some(order) =>
        sql"DELETE FROM ORDERS WHERE ID = $orderId".update.run.transact(xa).map(_ => Some(order))
      case None =>
        Task.now(None)
    }
  }
}

object DoobieOrderRepositoryInterpreter {
  def apply(): DoobieOrderRepositoryInterpreter = {
    val xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")
    new DoobieOrderRepositoryInterpreter(xa)
  }
}

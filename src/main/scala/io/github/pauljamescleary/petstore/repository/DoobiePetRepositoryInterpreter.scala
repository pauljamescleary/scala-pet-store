package io.github.pauljamescleary.petstore.repository

import doobie.util.transactor.Transactor
import fs2.Task
import io.github.pauljamescleary.petstore.model._
import doobie.imports._
import cats._
import cats.data._
import cats.implicits._
import fs2.interop.cats._
import shapeless._
import shapeless.record.Record

class DoobiePetRepositoryInterpreter(val xa: Transactor[Task]) extends PetRepositoryAlgebra[Task] {

  // This will clear the database on start.  Note, this would typically be done via something like FLYWAY (TODO)
  sql"""
    DROP TABLE IF EXISTS PET
  """.update.run.transact(xa).unsafeRun

  // The tags column is controversial, could be a lookup table.  For our purposes, indexing on tags to allow searching is fine
  sql"""
    CREATE TABLE PET (
      ID   SERIAL,
      NAME VARCHAR NOT NULL,
      CATEGORY VARCHAR NOT NULL,
      BIO  VARCHAR NOT NULL,
      STATUS VARCHAR NOT NULL,
      PHOTO_URLS VARCHAR NOT NULL,
      TAGS VARCHAR NOT NULL
    )
  """.update.run.transact(xa).unsafeRun

  /* We require tye StatusMeta to handle our ADT Status */
  private implicit val StatusMeta: Meta[Status] = Meta[String].nxmap(Status.apply, Status.nameOf)

  /* This is used to marshal our sets of strings */
  private implicit val SetStringMeta: Meta[Set[String]] = Meta[String].nxmap(str => str.split(',').toSet, strSet => strSet.mkString(","))

  def put(pet: Pet): Task[Pet] = {
    val insert: ConnectionIO[Pet] =
      for {
        id <- sql"REPLACE INTO PET (NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS) values (${pet.name}, ${pet.category}, ${pet.bio}, ${pet.status}, ${pet.photoUrls}, ${pet.tags})".update.withUniqueGeneratedKeys[Long]("ID")
      } yield pet.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(id: Long): Task[Option[Pet]] = {
    sql"""
      SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
        FROM PET
       WHERE ID = $id
     """.query[Pet].option.transact(xa)
  }

  def delete(id: Long): Task[Option[Pet]] = {
    get(id).flatMap {
      case Some(pet) =>
        sql"DELETE FROM PET WHERE ID = $id".update.run.transact(xa).map(_ => Some(pet))
      case None =>
        Task.now(None)
    }
  }

  def findByNameAndCategory(name: String, category: String): Task[Set[Pet]] = {
    sql"""SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
            FROM PET
           WHERE NAME = $name AND CATEGORY = $category
           """.query[Pet].list.transact(xa).map(_.toSet)
  }

  def list(pageSize: Int, offset: Int): Task[Seq[Pet]] = {
    sql"""SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
            FROM PET
            ORDER BY NAME LIMIT $offset,$pageSize""".query[Pet].list.transact(xa)
  }
}

object DoobiePetRepositoryInterpreter {

  /* Hardcoded to H2 for the time being */
  def apply(): DoobiePetRepositoryInterpreter = {
    val xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")
    new DoobiePetRepositoryInterpreter(xa)
  }
}

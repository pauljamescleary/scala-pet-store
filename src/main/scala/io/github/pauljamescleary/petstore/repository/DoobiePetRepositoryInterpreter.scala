package io.github.pauljamescleary.petstore.repository

import doobie.util.transactor.Transactor
import fs2.Task
import io.github.pauljamescleary.petstore.model._

import doobie.imports._
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._

class DoobiePetRepositoryInterpreter(val xa: Transactor[Task]) extends PetRepositoryAlgebra[Task] {

  // This will clear the database on start.  Note, this would typically be done via something like FLYWAY (TODO)
  sql"""
    DROP TABLE IF EXISTS pet
  """.update.run.transact(xa).unsafeRun

  sql"""
    CREATE TABLE pet (
      id   SERIAL,
      name VARCHAR NOT NULL,
      typ  VARCHAR NOT NULL,
      bio  VARCHAR NOT NULL
    )
  """.update.run.transact(xa).unsafeRun

  /* We require tye PetTypeMeta to handle our ADT PetType */
  implicit val PetTypeMeta: Meta[PetType] = Meta[String].nxmap(PetType.apply, PetType.nameOf)

  def put(pet: Pet): Task[Pet] = {
    val insert: ConnectionIO[Pet] =
      for {
        id <- sql"replace into pet (name, typ, bio) values (${pet.name}, ${pet.typ}, ${pet.bio})".update.withUniqueGeneratedKeys[Long]("id")
      } yield pet.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(id: Long): Task[Option[Pet]] = {
    sql"select name, typ, bio, id from pet where id = $id".query[Pet].option.transact(xa)
  }

  def delete(id: Long): Task[Option[Pet]] = {
    get(id).flatMap {
      case Some(pet) =>
        sql"delete from pet where id = $id".update.run.transact(xa).map(_ => Some(pet))
      case None =>
        Task.now(None)
    }
  }

  def findByNameAndType(name: String, typ: PetType): Task[Set[Pet]] = {
    sql"select name, typ, bio, id from pet where name = $name and typ = $typ".query[Pet].list.transact(xa).map(_.toSet)
  }

  def list(pageSize: Int, offset: Int): Task[Seq[Pet]] = {
    sql"select name, typ, bio, id from pet order by name limit $offset,$pageSize".query[Pet].list.transact(xa)
  }
}

object DoobiePetRepositoryInterpreter {

  /* Hardcoded to H2 for the time being */
  def apply(): DoobiePetRepositoryInterpreter = {
    val xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")
    new DoobiePetRepositoryInterpreter(xa)
  }
}

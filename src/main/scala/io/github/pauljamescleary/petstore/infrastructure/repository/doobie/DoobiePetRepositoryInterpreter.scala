package io.github.pauljamescleary.petstore.infrastructure.repository.doobie

import cats._
import cats.data._
import cats.implicits._
import doobie._
import doobie.implicits._
import io.github.pauljamescleary.petstore.domain.pets.{Pet, PetRepositoryAlgebra, PetStatus}
import SQLPagination._

private object PetSQL {
  /* We require type StatusMeta to handle our ADT Status */
  implicit val StatusMeta: Meta[PetStatus] =
    Meta[String].xmap(PetStatus.withName, _.entryName)

  /* This is used to marshal our sets of strings */
  implicit val SetStringMeta: Meta[Set[String]] = Meta[String]
    .xmap(str => str.split(',').toSet, strSet => strSet.mkString(","))

  def insert(pet: Pet) : Update0 = sql"""
    INSERT INTO PET (NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS)
    VALUES (${pet.name}, ${pet.category}, ${pet.bio}, ${pet.status}, ${pet.tags}, ${pet.photoUrls})
  """.update

  def update(pet: Pet, id: Long) : Update0 = sql"""
    UPDATE PET
    SET NAME = ${pet.name}, BIO = ${pet.bio}, STATUS = ${pet.status}, TAGS = ${pet.tags}, PHOTO_URLS = ${pet.photoUrls}
    WHERE id = $id
  """.update

  def select(id: Long) : Query0[Pet] = sql"""
    SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
    FROM PET
    WHERE ID = $id
  """.query

  def delete(id: Long) : Update0 = sql"""
    DELETE FROM PET WHERE ID = $id
  """.update

  def selectByNameAndCategory(name: String, category: String) : Query0[Pet] = sql"""
    SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
    FROM PET
    WHERE NAME = $name AND CATEGORY = $category
  """.query[Pet]

  def selectAll : Query0[Pet] = sql"""
    SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
    FROM PET
    ORDER BY NAME
  """.query

  def selectByStatus(statuses: NonEmptyList[PetStatus]) : Query0[Pet] = (
    sql"""
      SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
      FROM PET
      WHERE """ ++ Fragments.in(fr"STATUS", statuses)
  ).query

  def selectTagLikeString(tags: NonEmptyList[String]) : Query0[Pet] = {
    /* Handle dynamic construction of query based on multiple parameters */

    /* To piggyback off of comment of above reference about tags implementation, findByTag uses LIKE for partial matching
    since tags is (currently) implemented as a comma-delimited string */
    val tagLikeString: String = tags.toList.mkString("TAGS LIKE '%", "%' OR TAGS LIKE '%", "%'")
    (sql"""SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
         FROM PET
         WHERE """ ++ Fragment.const(tagLikeString))
      .query[Pet]
  }
}

class DoobiePetRepositoryInterpreter[F[_]: Monad](val xa: Transactor[F])
    extends PetRepositoryAlgebra[F] {
  import PetSQL._

  def create(pet: Pet): F[Pet] =
    insert(pet).withUniqueGeneratedKeys[Long]("ID").map(id => pet.copy(id = id.some)).transact(xa)

  def update(pet: Pet): F[Option[Pet]] = OptionT.fromOption[ConnectionIO](pet.id).semiflatMap(id =>
    PetSQL.update(pet, id).run.as(pet)
  ).value.transact(xa)

  def get(id: Long): F[Option[Pet]] = select(id).option.transact(xa)

  def delete(id: Long): F[Option[Pet]] = OptionT(get(id)).semiflatMap(pet =>
    PetSQL.delete(id).run.transact(xa).as(pet)
  ).value

  def findByNameAndCategory(name: String, category: String): F[Set[Pet]] =
    selectByNameAndCategory(name, category).to[List].transact(xa).map(_.toSet)

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)

  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    selectByStatus(statuses).to[List].transact(xa)

  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    selectTagLikeString(tags).to[List].transact(xa)
}

object DoobiePetRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobiePetRepositoryInterpreter[F] =
    new DoobiePetRepositoryInterpreter(xa)
}

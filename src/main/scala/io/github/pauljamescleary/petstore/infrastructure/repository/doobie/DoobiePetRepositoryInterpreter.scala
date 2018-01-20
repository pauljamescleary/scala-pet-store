package io.github.pauljamescleary.petstore.infrastructure.repository.doobie

import cats._
import cats.data._
import cats.implicits._
import doobie._
import doobie.implicits._
import io.github.pauljamescleary.petstore.domain.pets.{Pet, PetRepositoryAlgebra, PetStatus}

private object PetQueries {
  /* We require type StatusMeta to handle our ADT Status */
  private implicit val StatusMeta: Meta[PetStatus] =
    Meta[String].xmap(PetStatus.apply, PetStatus.nameOf)

  /* This is used to marshal our sets of strings */
  private implicit val SetStringMeta: Meta[Set[String]] = Meta[String]
    .xmap(str => str.split(',').toSet, strSet => strSet.mkString(","))

  def insert(pet: Pet) : Update0 = sql"""
    REPLACE INTO PET (NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS)
    VALUES (${pet.name}, ${pet.category}, ${pet.bio}, ${pet.status}, ${pet.tags}, ${pet.photoUrls})
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

  def selectPaginated(offset: Int, pageSize: Int) : Query0[Pet] = sql"""
    SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
    FROM PET
    ORDER BY NAME
    LIMIT $offset,$pageSize
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

  def put(pet: Pet): F[Pet] = {
    val insert: ConnectionIO[Pet] =
      for {
        id <- PetQueries.insert(pet).withUniqueGeneratedKeys[Long]("ID")
      } yield pet.copy(id = Some(id))
    insert.transact(xa)
  }

  def get(id: Long): F[Option[Pet]] = PetQueries.select(id).option.transact(xa)

  def delete(id: Long): F[Option[Pet]] =
    get(id).flatMap {
      case Some(pet) => PetQueries.delete(id).run.transact(xa).map(_ => pet.some)
      case None => none[Pet].pure[F]
    }

  def findByNameAndCategory(name: String, category: String): F[Set[Pet]] =
    PetQueries.selectByNameAndCategory(name, category).list.transact(xa).map(_.toSet)

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    PetQueries.selectPaginated(pageSize, offset).list.transact(xa)

  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    PetQueries.selectByStatus(statuses).list.transact(xa)

  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] = {
    PetQueries.selectTagLikeString(tags).list.transact(xa)
  }
}

object DoobiePetRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobiePetRepositoryInterpreter[F] =
    new DoobiePetRepositoryInterpreter(xa)
}

package io.github.pauljamescleary.petstore
package infrastructure.repository.inmemory

import scala.collection.concurrent.TrieMap
import scala.util.Random

import cats._
import cats.data.NonEmptyList
import cats.implicits._
import domain.pets.{Pet, PetRepositoryAlgebra, PetStatus}
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class PetRepositoryInMemoryInterpreter[F[_]: Applicative: Logger] extends PetRepositoryAlgebra[F] {
  private val cache = new TrieMap[Long, Pet]

  private val random = new Random

  def create(pet: Pet): F[Pet] = Logger[F].debug(s"Creating pet $pet") *> {
    val id = random.nextLong()
    val toSave = pet.copy(id = id.some)
    cache += (id -> pet.copy(id = id.some))
    toSave.pure[F]
  }

  def update(pet: Pet): F[Option[Pet]] = Logger[F].debug(s"Updating pet $pet") *> pet.id.traverse {
    id =>
      cache.update(id, pet)
      pet.pure[F]
  }

  def get(id: Long): F[Option[Pet]] = Logger[F].debug(s"Looking up pet for id $id") *>
    cache.get(id).pure[F]

  def delete(id: Long): F[Option[Pet]] =
    Logger[F].debug(s"Deleting pet with id $id") *> cache.remove(id).pure[F]

  def findByNameAndCategory(name: String, category: String): F[Set[Pet]] =
    Logger[F].debug(s"Looking up pet with name $name and category $category") *>
      cache.values
        .filter(p => p.name == name && p.category == category)
        .toSet
        .pure[F]

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    Logger[F].debug(s"Requested page list for pageSize $pageSize and offset $offset") *>
      cache.values.toList.sortBy(_.name).slice(offset, offset + pageSize).pure[F]

  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    Logger[F].debug(s"Requested pets with status $statuses") *>
      cache.values.filter(p => statuses.exists(_ == p.status)).toList.pure[F]

  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] =
    Logger[F].debug(s"Requested pets with tags $tags") *> {
      val tagSet = tags.toNes
      cache.values.filter(_.tags.exists(tagSet.contains(_))).toList.pure[F]
    }
}

object PetRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative: Sync](): Resource[F, PetRepositoryInMemoryInterpreter[F]] =
    Resource.eval(Slf4jLogger.create[F]).map { implicit logger: Logger[F] =>
      new PetRepositoryInMemoryInterpreter[F]()
    }
}

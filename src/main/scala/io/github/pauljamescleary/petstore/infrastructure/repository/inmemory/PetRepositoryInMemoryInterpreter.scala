package io.github.pauljamescleary.petstore
package infrastructure.repository.inmemory

import scala.collection.concurrent.TrieMap
import scala.util.Random

import cats._
import cats.data.NonEmptyList
import cats.implicits._
import domain.pets.{Pet, PetRepositoryAlgebra, PetStatus}

class PetRepositoryInMemoryInterpreter[F[_]: Applicative] extends PetRepositoryAlgebra[F] {
  private val cache = new TrieMap[Long, Pet]

  private val random = new Random

  def create(pet: Pet): F[Pet] = {
    val id = random.nextLong()
    val toSave = pet.copy(id = id.some)
    cache += (id -> pet.copy(id = id.some))
    toSave.pure[F]
  }

  def update(pet: Pet): F[Option[Pet]] = pet.id.traverse { id =>
    cache.update(id, pet)
    pet.pure[F]
  }

  def get(id: Long): F[Option[Pet]] = cache.get(id).pure[F]

  def delete(id: Long): F[Option[Pet]] = cache.remove(id).pure[F]

  def findByNameAndCategory(name: String, category: String): F[Set[Pet]] =
    cache.values
      .filter(p => p.name == name && p.category == category)
      .toSet
      .pure[F]

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    cache.values.toList.sortBy(_.name).slice(offset, offset + pageSize).pure[F]

  def findByStatus(statuses: NonEmptyList[PetStatus]): F[List[Pet]] =
    cache.values.filter(p => statuses.exists(_ == p.status)).toList.pure[F]

  def findByTag(tags: NonEmptyList[String]): F[List[Pet]] = {
    val tagSet = tags.toNes
    cache.values.filter(_.tags.exists(tagSet.contains(_))).toList.pure[F]
  }
}

object PetRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new PetRepositoryInMemoryInterpreter[F]()
}

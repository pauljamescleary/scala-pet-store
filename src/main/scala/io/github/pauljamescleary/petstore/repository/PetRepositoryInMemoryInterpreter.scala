package io.github.pauljamescleary.petstore.repository

import cats.effect.IO
import io.github.pauljamescleary.petstore.model.Pet

import scala.collection.concurrent.TrieMap
import scala.util.Random

object PetRepositoryInMemoryInterpreter extends PetRepositoryAlgebra[IO] {

  private val cache = new TrieMap[Long, Pet]

  private val random = new Random

  def put(pet: Pet): IO[Pet] =
    IO.pure {
      val toSave = if (pet.id.isDefined) pet else pet.copy(id = Some(random.nextLong))

      toSave.id.foreach { cache.put(_, toSave) }
      toSave
    }

  def get(id: Long): IO[Option[Pet]] = IO.pure(cache.get(id))

  def delete(id: Long): IO[Option[Pet]] = IO.pure(cache.remove(id))

  def findByNameAndCategory(name: String, category: String): IO[Set[Pet]] =
    IO.pure {
      cache.values.filter(p => p.name == name && p.category == category).toSet
    }

  def list(pageSize: Int, offset: Int): IO[Seq[Pet]] =
    IO.pure {
      cache.values.toSeq.sortBy(_.name).slice(offset, offset + pageSize)
    }
}

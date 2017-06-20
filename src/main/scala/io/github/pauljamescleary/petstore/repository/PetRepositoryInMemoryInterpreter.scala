package io.github.pauljamescleary.petstore.repository

import fs2.Task
import io.github.pauljamescleary.petstore.model.Pet

import scala.collection.concurrent.TrieMap
import scala.util.Random

object PetRepositoryInMemoryInterpreter extends PetRepositoryAlgebra[Task] {

  private val cache = new TrieMap[Long, Pet]

  private val random = new Random

  def put(pet: Pet): Task[Pet] =
    Task.now {
      val toSave = if (pet.id.isDefined) pet else pet.copy(id = Some(random.nextLong))

      toSave.id.foreach { cache.put(_, toSave) }
      toSave
    }

  def get(id: Long): Task[Option[Pet]] = Task.now(cache.get(id))

  def delete(id: Long): Task[Option[Pet]] = Task.now(cache.remove(id))

  def findByNameAndCategory(name: String, category: String): Task[Set[Pet]] =
    Task.now {
      cache.values.filter(p => p.name == name && p.category == category).toSet
    }

  def list(pageSize: Int, offset: Int): Task[Seq[Pet]] =
    Task.now {
      cache.values.toSeq.sortBy(_.name).slice(offset, offset + pageSize)
    }
}

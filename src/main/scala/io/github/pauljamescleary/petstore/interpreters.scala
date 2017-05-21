package io.github.pauljamescleary.petstore

import fs2.Task

import scala.collection.concurrent.TrieMap
import scala.util.Random

object PetRepositoryTaskIntepreter extends PetRepositoryAlgebra[Task] {

  private val cache = new TrieMap[Long, Pet]

  private val random = new Random

  def put(pet: Pet): Task[Pet] =
    Task.now {
      val toSave = if (pet.id.isDefined) pet else pet.copy(id = Some(random.nextLong))

      toSave.id.foreach { cache.put(_, toSave) }
      toSave
    }

  def get(id: Long): Task[Option[Pet]] = Task.now(cache.get(id))
}
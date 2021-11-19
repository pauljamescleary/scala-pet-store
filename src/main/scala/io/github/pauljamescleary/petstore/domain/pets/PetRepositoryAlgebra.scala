package io.github.pauljamescleary.petstore.domain.pets

import cats.data.NonEmptyList
import cats.effect.IO

trait PetRepositoryAlgebra {
  def create(pet: Pet): IO[Pet]

  def update(pet: Pet): IO[Option[Pet]]

  def get(id: Long): IO[Option[Pet]]

  def delete(id: Long): IO[Option[Pet]]

  def findByNameAndCategory(name: String, category: String): IO[Set[Pet]]

  def list(pageSize: Int, offset: Int): IO[List[Pet]]

  def findByStatus(status: NonEmptyList[PetStatus]): IO[List[Pet]]

  def findByTag(tags: NonEmptyList[String]): IO[List[Pet]]
}

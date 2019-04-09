package io.github.pauljamescleary.petstore.domain.pets

import scala.language.higherKinds
import cats.data.NonEmptyList
import cats.effect.Bracket

trait PetRepositoryAlgebra[F[_]] {
  type B = Bracket[F, Throwable]

  def create(pet: Pet)(implicit b: B): F[Pet]

  def update(pet: Pet)(implicit b: B) : F[Option[Pet]]

  def get(id: Long)(implicit b: B): F[Option[Pet]]

  def delete(id: Long)(implicit b: B): F[Option[Pet]]

  def findByNameAndCategory(name: String, category: String)(implicit b: B): F[Set[Pet]]

  def list(pageSize: Int, offset: Int)(implicit b: B): F[List[Pet]]

  def findByStatus(status: NonEmptyList[PetStatus])(implicit b: B): F[List[Pet]]

  def findByTag(tags: NonEmptyList[String])(implicit b: B): F[List[Pet]]
}

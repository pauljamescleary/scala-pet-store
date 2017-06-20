package io.github.pauljamescleary.petstore.repository

import io.github.pauljamescleary.petstore.model.Pet

import scala.language.higherKinds

trait PetRepositoryAlgebra[F[_]] {

  def put(pet: Pet): F[Pet]

  def get(id: Long): F[Option[Pet]]

  def delete(id: Long): F[Option[Pet]]

  def findByNameAndCategory(name: String, category: String): F[Set[Pet]]

  def list(pageSize: Int, offset: Int): F[Seq[Pet]]
}

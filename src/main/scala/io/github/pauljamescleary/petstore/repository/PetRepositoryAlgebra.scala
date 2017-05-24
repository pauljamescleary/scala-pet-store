package io.github.pauljamescleary.petstore.repository

import io.github.pauljamescleary.petstore.model.{Pet, PetType}

import scala.language.higherKinds

trait PetRepositoryAlgebra[F[_]] {

  def put(pet: Pet): F[Pet]

  def get(id: Long): F[Option[Pet]]

  def delete(id: Long): F[Option[Pet]]

  def findByNameAndType(name: String, typ: PetType): F[Set[Pet]]

  def list(pageSize: Int, offset: Int): F[Seq[Pet]]
}

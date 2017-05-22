package io.github.pauljamescleary.petstore.repository

import fs2.Task
import io.github.pauljamescleary.petstore.model

import doobie.imports._
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._

class DoobiePetRepository extends PetRepositoryAlgebra[Task] {

  def put(pet: model.Pet): Task[model.Pet] = ???

  def get(id: Long): Task[Option[model.Pet]] = ???

  def delete(id: Long): Task[Option[model.Pet]] = ???

  def findByNameAndType(name: String, typ: model.PetType): Task[Set[model.Pet]] = ???
}

package io.github.pauljamescleary.petstore.domain.model

case class Pet(
    name: String,
    category: String,
    bio: String,
    status: PetStatus = Available,
    tags: Set[String] = Set.empty,
    photoUrls: Set[String] = Set.empty,
    id: Option[Long] = None
)

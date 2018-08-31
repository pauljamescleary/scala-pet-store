package io.github.pauljamescleary.petstore.domain.pets

case class Pet(
    name: String,
    category: String,
    bio: String,
    status: PetStatus = PetStatus.Available,
    tags: Set[String] = Set.empty,
    photoUrls: Set[String] = Set.empty,
    id: Option[Long] = None
)

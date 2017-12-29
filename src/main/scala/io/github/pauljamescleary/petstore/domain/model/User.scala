package io.github.pauljamescleary.petstore.domain.model

case class User(
    userName: String,
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    phone: String,
    id: Option[Long] = None
)

package io.github.pauljamescleary.petstore.model

case class User(
    userName: String,
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    phone: String,
    id: Option[Long] = None
)

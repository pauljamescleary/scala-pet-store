package io.github.pauljamescleary.petstore
package domain.authentication

import domain.users.{Role, User}
import tsec.passwordhashers.PasswordHash

final case class LoginRequest(
    userName: String,
    password: String,
)

final case class SignupRequest(
    userName: String,
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    phone: String,
    role: Role,
) {
  def asUser[A](hashedPassword: PasswordHash[A]): User = User(
    userName,
    firstName,
    lastName,
    email,
    hashedPassword.toString,
    phone,
    role = role,
  )
}

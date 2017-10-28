package io.github.pauljamescleary.petstore.model

/*
 * It is always nice to give name to what can go wrong on the
 * code in the context of your application.
 *
 * You could certainly create or use the same extensible system than for
 * PetValidation.
 */
sealed abstract class PetError(msg: String) extends Exception(msg)

final case object PetError {

  final case class Parsing(msg: String) extends PetError(msg)

}

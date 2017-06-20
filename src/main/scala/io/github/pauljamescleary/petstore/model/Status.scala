package io.github.pauljamescleary.petstore.model

sealed trait Status
case object Available extends Status
case object Pending extends Status
case object Adopted extends Status

object Status {
  def apply(name: String): Status = name match {
    case "Available" => Available
    case "Pending" => Pending
    case "Adopted" => Adopted
  }

  def nameOf(status: Status): String = status match {
    case Available => "Available"
    case Pending => "Pending"
    case Adopted => "Adopted"
  }
}

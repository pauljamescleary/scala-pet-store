package io.github.pauljamescleary.petstore.model

sealed trait OrderStatus extends Product with Serializable

/*
 * Putting case object in the object with the same name
 * as you sealed trait is a common practice that allows
 * - to qualify named (better discoverability for other dev)
 *   ex: OrderStatus.Approved is self-telling, don't need to
 *   wonder if it is not Status.Approved)
 * - to use libraries which needs that convention
 *   (ex: sealerate is a neat little tool that I used frequently
 *   when I need to iterate on all cases of the ADT)
 * - it is widelly use in scalaz/monix/shapeless/etc to better
 *   managed implicits scoping
 */
object OrderStatus {

  case object Approved extends OrderStatus
  /**
   * to be known: case object into other object are not final from the point
   * of view of the JVM: javap OrderedStatus$Delivered =>
   *   public class io.github.pauljamescleary.petstore.model.OrderStatus$Delivered$ ...
   * So you may want to use "final case object ...", that can
   * give the jvm better insight on optimisation.
   */
  case object Delivered extends OrderStatus
  //given as example: javap OrderedStatus$Placed
  //  public final class io.github.pauljamescleary.petstore.model.OrderStatus$Placed$
  final case object Placed extends OrderStatus

  //here, you can call
  def allValues = ca.mrvisser.sealerate.values[OrderStatus]


  // be aware that the time for search on name increase with
  // all name. An alternative is to add a field "name" to
  // OrderedStatus, but you loose the total separation between
  // your pure ADT and function on it. Tradeoff :)
  def nameOf(status: OrderStatus): String = status match {
    case Approved => "Approved"
    case Delivered => "Delivered"
    case Placed => "Placed"
  }

  /*
   * This one should return an Either because it can fail.
   * You can define it as a fonction of nameOf // allValues
   * so that you are sure to never forget to add new values
   * if you extend OrderedStatus (as you won't have the
   * protection of total matching that you have in nameOf)
   */
  def applyUnsafe(name: String): OrderStatus = name match {
    case "Approved" => Approved
    case "Delivered" => Delivered
    case "Placed" => Placed
  }

  //ex alternative impl to apply, putting in ligth the fact that it
  //can fail and using allValues to have it automatically extended
  //with the ADT
  def apply(name: String): Either[PetError, OrderStatus] = allValues.find(os => nameOf(os) == name) match {
    case None     => Left(PetError.Parsing(s"No OrderStatus matching value '${name}'. Expecting one of: ${allValues.map(nameOf).mkString}"))
    case Some(os) => Right(os)
  }
}

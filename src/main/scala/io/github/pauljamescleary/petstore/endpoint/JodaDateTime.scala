package io.github.pauljamescleary.petstore.endpoint

import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object JodaDateTime {
  // you should always add ZZ for the timezone :)
  private val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZ"
  implicit val decodeDateTime: Decoder[DateTime] = Decoder.instance { cursor =>
    cursor.as[String].flatMap { dateTime =>
      Right(this.fromString(dateTime))
    }
  }

  implicit val encodeDateTime: Encoder[DateTime] = Encoder.instance { dateTime: DateTime =>
    Json.fromString(this.toString(dateTime))
  }

  def toString(dateTime: DateTime): String = dateTime.toString(dateFormat)

  // from string can fail. You should never authorize your code to throw exception
  // (appart from "system" error. What is "system" (i.e, out of the scope errors is depending
  // from you app level. For ex, generaly, all Thread related exception are most likelly out
  // of the scope of your business logic and you don't want to manage them. But it is totally ok
  // for Akka/monix/etc to take care of them and transform them on "business" error. Or when
  // you are in a IO stack)
  def fromString(dateTime: String): DateTime =
    DateTime.parse(dateTime, DateTimeFormat.forPattern(dateFormat))
}

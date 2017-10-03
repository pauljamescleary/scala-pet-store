package io.github.pauljamescleary.petstore.endpoint

import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object JodaDateTime {
  private val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
  implicit val decodeDateTime: Decoder[DateTime] = Decoder.instance { cursor =>
    cursor.as[String].flatMap { dateTime =>
      Right(this.fromString(dateTime))
    }
  }

  implicit val encodeDateTime: Encoder[DateTime] = Encoder.instance {
    dateTime: DateTime =>
      Json.fromString(this.toString(dateTime))
  }

  def toString(dateTime: DateTime): String = dateTime.toString(dateFormat)
  def fromString(dateTime: String): DateTime =
    DateTime.parse(dateTime, DateTimeFormat.forPattern(dateFormat))
}

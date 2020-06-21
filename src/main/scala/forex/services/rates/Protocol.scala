package forex.services.rates

import java.time.LocalDateTime

import forex.domain.Currency
import io.circe.{ Decoder, HCursor }

object Protocol {
  final case class OneFrameResponse(from: Currency, to: Currency, price: BigDecimal, timeStamp: LocalDateTime)

  implicit val decoder: Decoder[OneFrameResponse] = (c: HCursor) =>
    for {
      from <- c.downField("from").as[String].map(Currency.fromString)
      to <- c.downField("to").as[String].map(Currency.fromString)
      price <- c.downField("price").as[BigDecimal]
      timestamp <- c.downField("time_stamp").as[LocalDateTime]
    } yield OneFrameResponse(from, to, price, timestamp)

}

package forex.services.rates

import java.math.MathContext
import java.time.Instant

import forex.domain.Currency
import io.circe.{Decoder, HCursor}

object Protocol {
  final case class OneFrameResponse(from: Currency, to: Currency, price: BigDecimal, timeStamp: Instant)

  implicit val decoder: Decoder[OneFrameResponse] = (c: HCursor) =>
    for {
      from <- c.downField("from").as[String].map(Currency.fromString)
      to <- c.downField("to").as[String].map(Currency.fromString)
      price <- c.downField("price").as[Double].map(dbl => {val mc = new MathContext(30); BigDecimal(dbl, mc)})
      timestamp <- c.downField("time_stamp").as[Instant]
    } yield OneFrameResponse(from, to, price, timestamp)

}

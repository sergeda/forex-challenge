package forex.http
package rates

import forex.domain.Currency.show
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.semiauto._

object Protocol {

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val currencyDecoder: Decoder[Currency] =
    (c: HCursor) => c.as[String].map(Currency.fromString)

  implicit val pairCodec: Codec[Pair] =
    deriveCodec[Pair]

  implicit val rateCodec: Codec[Rate] =
    deriveCodec[Rate]

  implicit val responseCodec: Codec[GetApiResponse] =
    deriveCodec[GetApiResponse]

}

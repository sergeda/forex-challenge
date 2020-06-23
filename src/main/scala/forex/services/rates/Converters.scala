package forex.services.rates

import java.time.{Instant, OffsetDateTime}
import java.util.TimeZone

import forex.domain._
import forex.services.rates.Protocol.OneFrameResponse

object Converters {

  private[rates] implicit class TimestampOps(val instant: Instant) extends AnyVal {
    def asTimestamp: Timestamp = {
      val timeZoneId     = TimeZone.getDefault.toZoneId
      val zoneOffset     = timeZoneId.getRules.getOffset(instant)
      val offsetDateTime = OffsetDateTime.ofInstant(instant, zoneOffset)
      Timestamp(offsetDateTime)
    }
  }

  private[rates] implicit class RateOps(val response: OneFrameResponse) extends AnyVal {
    def asRate: Rate =
      Rate(Rate.Pair(response.from, response.to), Price(response.price), response.timeStamp.asTimestamp)

  }

}

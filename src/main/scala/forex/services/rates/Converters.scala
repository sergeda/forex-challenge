package forex.services.rates

import java.time.OffsetDateTime
import java.util.TimeZone
import forex.domain._
import forex.services.rates.Protocol.OneFrameResponse

object Converters {

  private[rates] implicit class RateOps(val response: OneFrameResponse) extends AnyVal {
    def asRate: Rate = {
      val timeZoneId     = TimeZone.getDefault.toZoneId
      val zoneOffset     = timeZoneId.getRules.getOffset(response.timeStamp)
      val offsetDateTime = OffsetDateTime.of(response.timeStamp, zoneOffset)
      Rate(Rate.Pair(response.from, response.to), Price(response.price), Timestamp(offsetDateTime))
    }

  }

}

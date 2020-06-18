package forex.http.rates

import cats.effect.IO
import forex.config.ApplicationConfig
import forex.domain.Currency.{ CAD, CHF }
import forex.domain.Price
import forex.http.rates.Protocol._
import forex.{ HttpTestSpec, Module }
import org.http4s.implicits._
import org.http4s.{ Method, Request, Status }
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec

class RatesHttpRoutesSpec extends AsyncFreeSpec with HttpTestSpec with AsyncIOSpec {

  val config = ConfigSource.default.at("app").loadOrThrow[ApplicationConfig]

  val httpModule = new Module[IO](config)

  "Rate route " - {
    "should return Dummy response" in {

      customAssertHttp(
        httpModule.httpApp,
        Request(method = Method.GET, uri = uri"/rates?from=CHF&to=CAD")
      )(
        Status.Ok,
        (response: GetApiResponse) =>
          response.from == CHF && response.to == CAD && response.price == Price(BigDecimal(100))
      )

    }
  }

}

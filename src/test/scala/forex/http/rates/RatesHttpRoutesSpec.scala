package forex.http.rates

import cats.effect.IO
import forex.config.ApplicationConfig
import forex.domain.Currency.{ CAD, CHF }
import forex.domain.Price
import forex.http.rates.Protocol._
import forex.{ HttpTestAssertions, Module }
import org.http4s.implicits._
import org.http4s.{ Method, Request, Status }
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class RatesHttpRoutesSpec extends AsyncFreeSpec with HttpTestAssertions with AsyncIOSpec with Matchers {

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

    "should return BadRequest with error message in json format if incorrect currency specified" in {

      assertHttp(
        httpModule.httpApp,
        Request(method = Method.GET, uri = uri"/rates?from=CHF&to=AAA")
      )(
        Status.BadRequest,
        ErrorResponse("Incorrect currency specified")
      )

    }

    "should return NotFound if some parameter is missing" in {

      assertHttpStatus(
        httpModule.httpApp,
        Request(method = Method.GET, uri = uri"/rates?from=CHF")
      )(
        Status.NotFound
      )

    }
  }

}

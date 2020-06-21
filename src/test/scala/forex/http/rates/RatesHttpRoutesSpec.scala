package forex.http.rates

import cats.effect.IO
import cats.syntax.either._
import forex.HttpTestAssertions
import forex.domain.Currency.{ EUR, USD }
import forex.domain.{ Price, Rate, Timestamp }
import forex.http.rates.Protocol._
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors
import org.http4s.HttpApp
import org.scalatest.freespec.AnyFreeSpec
import forex.programs.rates.errors.Error.RateLookupFailed
import org.http4s.implicits._
import org.http4s.{ Method, Request, Status }
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers

class RatesHttpRoutesSpec extends AnyFreeSpec with HttpTestAssertions with Matchers with MockFactory {

  val ratesProgram            = stub[RatesProgram[IO]]
  val httpRoutes: HttpApp[IO] = new RatesHttpRoutes[IO](ratesProgram).routes.orNotFound

  "Rate route " - {
    "should return correct formatted response" in {

      (ratesProgram.get(_: GetRatesRequest)).when(*) returns IO.pure(
        Rate(Rate.Pair(USD, EUR), Price(BigDecimal(100)), Timestamp.now).asRight[errors.Error]
      )

      customAssertHttp(
        httpRoutes,
        Request(method = Method.GET, uri = uri"/rates?from=CHF&to=CAD")
      )(
        Status.Ok,
        (response: GetApiResponse) =>
          response.from == USD && response.to == EUR && response.price == Price(BigDecimal(100))
      )

    }

    "should return BadRequest with error message in json format if incorrect currency specified" in {

      assertHttp(
        httpRoutes,
        Request(method = Method.GET, uri = uri"/rates?from=CHF&to=AAA")
      )(
        Status.BadRequest,
        ErrorResponse("Incorrect currency specified")
      )

    }

    "should return page with error message in json format if no data can be retrieved" in {
      (ratesProgram.get(_: GetRatesRequest)).when(*) returns IO.pure(
        RateLookupFailed("Can't retrieve data").asLeft[Rate]
      )

      assertHttp(
        httpRoutes,
        Request(method = Method.GET, uri = uri"/rates?from=CHF&to=USD")
      )(
        Status.Ok,
        ErrorResponse("Can't retrieve data")
      )

    }

    "should return NotFound if some parameter is missing" in {

      assertHttpStatus(
        httpRoutes,
        Request(method = Method.GET, uri = uri"/rates?from=CHF")
      )(
        Status.NotFound
      )

    }
  }

}

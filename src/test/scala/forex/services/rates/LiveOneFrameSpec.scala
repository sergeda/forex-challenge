package forex.services.rates

import java.time.Instant

import forex.services.rates.Converters._
import cats.data.NonEmptyList
import cats.effect.{ ContextShift, IO }
import forex.config.OneFrameConfig
import forex.domain.Currency.{ CHF, EUR, USD }
import forex.domain.{ Price, Rate }
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.interpreters.LiveOneFrame
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ EitherValues, PrivateMethodTester }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.{ NothingT, Response, SttpBackend }
import sttp.model.StatusCode
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext.global

class LiveOneFrameSpec extends AnyFreeSpec with EitherValues with MockFactory with Matchers with PrivateMethodTester {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  val jsonResponse =
    """[{"from":"EUR","to":"CHF","bid":0.6262930151477809,"ask":0.041126043135137236,"price":0.333709529141459068,"time_stamp":"2020-06-22T07:54:49.451Z"}]"""
  val multipleResultsJsonResponse =
    """[{"from":"CHF","to":"EUR","bid":0.27822164078398093,"ask":0.019623759432511045,"price":0.1489227001082459875,"time_stamp":"2020-06-22T13:04:42.912Z"},{"from":"USD","to":"EUR","bid":0.9460985783654299,"ask":0.33446008550191075,"price":0.640279331933670325,"time_stamp":"2020-06-22T13:04:42.912Z"}]"""
  val instant = Instant.parse("2020-06-22T07:54:49.451Z")
  val successResponse = Response(
    jsonResponse,
    StatusCode.Ok,
    StatusCode.Ok.toString(),
    Vector(),
    List.empty[Response[Unit]]
  )

  val forbiddenResponse = Response(
    """{"error":"Forbidden"}""",
    StatusCode.Ok,
    StatusCode.Ok.toString(),
    Vector(),
    List.empty[Response[Unit]]
  )

  implicit val backend: SttpBackend[IO, Nothing, NothingT] =
    AsyncHttpClientCatsBackend.stub[IO].whenRequestMatchesPartial {
      case r if r.uri.toString().endsWith("rates?pair=EURCHF") =>
        successResponse
      case r if r.uri.toString().endsWith("rates?pair=CHFEUR&pair=USDEUR") =>
        successResponse.copy(body = multipleResultsJsonResponse)
      case r if r.uri.toString().endsWith("rates?pair=CHFEUR") =>
        forbiddenResponse
    }

  val config = ConfigSource.default
    .at("app.oneframe")
    .loadOrThrow[OneFrameConfig]

  val oneFrame = new LiveOneFrame[IO](config)

  val rate = Rate(Rate.Pair(EUR, CHF), Price(BigDecimal(0.333709529141459068)), instant.asTimestamp)

  "OneFrame " - {
    "should correctly decode json response from OneFrame" in {

      val decodeContent = PrivateMethod[Either[Error, NonEmptyList[Rate]]]('decodeContent)
      (oneFrame invokePrivate decodeContent(jsonResponse)) should be(Right(NonEmptyList(rate, List.empty)))
    }

    "should correctly return results if data is available" in {

      val result: Either[errors.Error, Rate] = oneFrame.get(Rate.Pair(EUR, CHF)).unsafeRunSync()
      assert(result.right.value === rate)
    }

    "should correctly return multiple results if data is available" in {

      val result: Either[errors.Error, NonEmptyList[Rate]] =
        oneFrame.get(NonEmptyList(Rate.Pair(CHF, EUR), List(Rate.Pair(USD, EUR)))).unsafeRunSync()
      assert(result.right.value.length === 2)
    }

    "should return correct error if OneFrame site return incorrect content" in {

      val result: Either[errors.Error, Rate] = oneFrame.get(Rate.Pair(CHF, EUR)).unsafeRunSync()
      assert(result.left.value === OneFrameLookupFailed("""Incorrect response from OneFrame: {"error":"Forbidden"}"""))
    }

    "should correctly form request uri to OneFrame" in {

      val formUrl = PrivateMethod[String]('formUrl)
      val list    = NonEmptyList(Rate.Pair(CHF, EUR), List(Rate.Pair(USD, EUR)))
      (oneFrame invokePrivate formUrl(list)) should be(config.url + "pair=CHFEUR&pair=USDEUR")
    }

  }
}

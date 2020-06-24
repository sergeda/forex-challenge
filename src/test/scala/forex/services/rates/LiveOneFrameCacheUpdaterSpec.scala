package forex.services.rates

import cats.data.NonEmptyList
import cats.effect.{ ContextShift, IO, Timer }
import forex.domain.Currency.{ CHF, EUR, USD }
import forex.domain.{ Price, Rate, Timestamp }
import forex.domain.Rate.Pair
import forex.services.cache.Cache
import cats.syntax.either._
import forex.services.rates.interpreters.LiveOneFrameCacheUpdater
import org.scalamock.scalatest.MockFactory
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.global

class LiveOneFrameCacheUpdaterSpec extends AnyFreeSpec with EitherValues with MockFactory with Matchers {
  val cacheIO: Cache[IO]     = mock[Cache[IO]]
  val oneFrame: OneFrame[IO] = mock[OneFrame[IO]]

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]               = IO.timer(global)

  val updater = LiveOneFrameCacheUpdater.of(cacheIO, oneFrame)
  val oneFrameResult = NonEmptyList(
    Rate(Pair(CHF, EUR), Price(0.148), Timestamp.now),
    List(Rate(Pair(USD, EUR), Price(0.64), Timestamp.now))
  )

  "OneFrameCacheUpdater " - {
    "should correctly update cache with data from oneFrame" in {
      (oneFrame
        .get(_: NonEmptyList[Rate.Pair], _: Boolean))
        .expects(*, *)
        .returning(IO.pure(oneFrameResult.asRight[errors.Error]))
        .once()
      (cacheIO.put(_: String, _: Rate)).expects(*, *).returning(IO.unit).twice()

      val result = updater.update(List(Pair(USD, EUR), Pair(CHF, EUR))).unsafeRunSync()
      result shouldBe defined
      result.get.length shouldBe 2
    }
  }
}

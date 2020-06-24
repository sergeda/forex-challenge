package forex.services.rates

import cats.effect.IO
import cats.syntax.either._
import cats.syntax.option._
import forex.domain.Currency.{ EUR, USD }
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.cache.Cache
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.interpreters.LiveAlgebra
import org.scalamock.scalatest.MockFactory
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class LiveAlgebraSpec extends AnyFreeSpec with EitherValues with MockFactory with Matchers {

  val cacheIO: Cache[IO]     = stub[Cache[IO]]
  val oneFrame: OneFrame[IO] = stub[OneFrame[IO]]

  val algebra = new LiveAlgebra(cacheIO, oneFrame)

  val rate = Rate(Rate.Pair(USD, EUR), Price(BigDecimal(100)), Timestamp.now)

  "Algebra " - {
    "should correctly return results if data in the cache" in {

      (cacheIO.get(_: String)).when("USDEUR") returns IO.pure(rate.some)

      val result = algebra.get(Rate.Pair(USD, EUR)).unsafeRunSync()
      result.right.value shouldBe rate
    }

    "should return result calling OneFrame service if data is not in the cache" in {

      (cacheIO.get(_: String)).when("USDEUR") returns IO.pure(none[Rate])
      (oneFrame.get(_: Rate.Pair)).when(Rate.Pair(USD, EUR)) returns IO.pure(rate.asRight[errors.Error])

      algebra.get(Rate.Pair(USD, EUR)).unsafeRunSync().right.value shouldBe rate
    }

    "should return correct error if no value in the cache and call to OneFrame service has failed" in {
      val error = OneFrameLookupFailed("{\"error\":\"Forbidden\"}")
      (cacheIO.get(_: String)).when("USDEUR") returns IO.pure(none[Rate])
      (oneFrame.get(_: Rate.Pair)).when(Rate.Pair(USD, EUR)) returns IO.pure(error.asLeft[Rate])

      algebra.get(rate.pair).unsafeRunSync().left.value shouldBe error
    }

  }
}

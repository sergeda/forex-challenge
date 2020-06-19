package forex.services.cache
import cats.effect.IO
import cats.syntax.parallel.catsSyntaxParallelTraverse
import cats.instances.list._
import forex.TestExecutionContextSpec
import forex.config.ApplicationConfig
import forex.services.cache.interpreters.LiveCache
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import scala.concurrent.duration._

class LiveCacheSpec extends TestExecutionContextSpec {

  val config = ConfigSource.default
    .at("app")
    .loadOrThrow[ApplicationConfig]
    .cache

  val cacheIO: IO[Cache[IO, String, Int]] = LiveCache.of[IO, String, Int](config)

  val cacheInsert: ((String, Int)) => IO[Unit] = data => cacheIO.flatMap(_.put(data._1, data._2))

  val getFromCache: String => IO[Option[Int]] = key => cacheIO.flatMap(_.get(key))

  "Cache " - {
    "should correctly put records in parallel" in {

      val dataToInsert = List(("CHFCAD", 80), ("USDJPY", 100), ("JPYUSD", 50))

      val result = for {
        _ <- dataToInsert.parTraverse(cacheInsert)
        cacheContent <- dataToInsert.map(_._1).parTraverse(getFromCache)
      } yield cacheContent.flatten

      executionContext.tick(2.seconds)
      result.asserting(_.length shouldBe 3)
    }

    "should correctly clean up records" in {

      cacheInsert(("CHFCAD", 80)).unsafeToFuture()
      executionContext.tick(2.seconds)
      cacheInsert(("USDJPY", 100)).unsafeToFuture()
      executionContext.tick(3.seconds)

      getFromCache("CHFCAD").asserting(_ shouldBe empty)
      getFromCache("USDJPY").asserting(_ shouldBe defined)

      executionContext.tick(3.seconds)
      getFromCache("USDJPY").asserting(_ shouldBe empty)
    }

  }
}

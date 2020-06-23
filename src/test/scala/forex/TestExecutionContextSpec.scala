package forex

import cats.effect.{ContextShift, IO, Timer}
import cats.effect.laws.util.TestContext
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AnyFreeSpec

trait TestExecutionContextSpec extends AnyFreeSpec with Matchers {
  val executionContext: TestContext             = TestContext()
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(executionContext)
  implicit val ioTimer: Timer[IO]               = executionContext.timer

}

package forex

import cats.effect.{ContextShift, IO, Timer}
import cats.effect.laws.util.TestContext
import cats.effect.testing.scalatest.AssertingSyntax
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AnyFreeSpec

trait TestExecutionContextSpec extends AnyFreeSpec with Matchers with AssertingSyntax {
  val executionContext: TestContext             = TestContext.apply()
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(executionContext)
  implicit val ioTimer: Timer[IO]               = IO.timer(executionContext)

}

package forex

import cats.effect.IO
import forex.http._
import io.circe._
import io.circe.syntax._
import org.http4s._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

trait HttpTestAssertions { self: Matchers =>

  def assertHttp[A: Encoder](routes: HttpApp[IO], req: Request[IO])(
      expectedStatus: Status,
      expectedBody: A
  ): IO[Assertion] =
    routes.run(req).flatMap { resp =>
      resp.as[Json].map { json =>
        assert(resp.status == expectedStatus && json == expectedBody.asJson)
      }
    }

  def customAssertHttp[A: Decoder](routes: HttpApp[IO], req: Request[IO])(
      expectedStatus: Status,
      check: A => Boolean
  ): IO[Assertion] =
    routes.run(req).flatMap { resp =>
      resp.as[Json].map { json =>
        assert(resp.status == expectedStatus && json.as[A].isRight && check(json.as[A].toOption.get))
      }
    }

  def assertHttpStatus(routes: HttpApp[IO], req: Request[IO])(expectedStatus: Status) =
    routes.run(req).map { resp =>
      assert(resp.status == expectedStatus)
    }

  def assertHttpFailure(routes: HttpApp[IO], req: Request[IO]) =
    routes.run(req).attempt.map {
      case Left(_)  => assert(true)
      case Right(_) => fail("expected a failure")
    }

}

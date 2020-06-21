package forex.http
package rates

import cats.Applicative
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.instances.option._
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(maybeFrom) +& ToQueryParam(maybeTo) =>
      Applicative[Option]
        .map2(maybeFrom, maybeTo)(
          (from, to) =>
            rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
              case Left(error) => Ok(ErrorResponse(error.getMessage))
              case Right(rate) => Ok(rate.asGetApiResponse)
          }
        )
        .getOrElse(BadRequest(ErrorResponse("Incorrect currency specified")))

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}

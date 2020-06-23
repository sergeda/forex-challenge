package forex

import cats.effect.{ Concurrent, Timer }
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import forex.services.cache.Cache
import forex.services.cache.interpreters.LiveCache
import forex.services.rates.interpreters.LiveOneFrame
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }
import sttp.client.{ NothingT, SttpBackend }

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig)(
    implicit val backend: SttpBackend[F, Nothing, NothingT]
) {

  private val ratesCache: F[Cache[F]] = LiveCache.of[F](config.cache.expire)

  private val oneFrameService: OneFrameService[F] = new LiveOneFrame[F](config.oneframe)

  private val ratesService: RatesService[F] = RatesServices.live[F](ratesCache, oneFrameService)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}

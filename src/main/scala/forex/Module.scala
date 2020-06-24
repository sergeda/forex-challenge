package forex

import cats.effect.{ Concurrent, Fiber, Timer }
import cats.effect.syntax.concurrent._
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import forex.services.cache.Cache
import forex.services.cache.interpreters.LiveCache
import forex.services.rates.interpreters.LiveOneFrameCacheUpdater
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }
import sttp.client.{ NothingT, SttpBackend }

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig)(
    implicit val backend: SttpBackend[F, Nothing, NothingT]
) {

  private val ratesCache: Cache[F] = LiveCache.of[F](config.cache.expire)

  private val oneFrameService: OneFrameService[F] = RatesServices.liveOneFrame(config.oneframe)

  private val ratesService: RatesService[F] = RatesServices.liveAlgebra[F](ratesCache, oneFrameService)

  private val ratesUpdaterService = RatesServices.liveOneFrameCacheUpdater(ratesCache, oneFrameService)

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

  val ratesUpdateScheduler: F[Fiber[F, Unit]] =
    LiveOneFrameCacheUpdater.schedule(config.oneframe.update, ratesUpdaterService).start

}

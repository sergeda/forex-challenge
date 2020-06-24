package forex.services.rates

import cats.effect.{ Concurrent, Timer }
import cats.{ Functor, Monad }
import forex.config.OneFrameConfig
import forex.services.cache.Cache
import interpreters._
import sttp.client.{ NothingT, SttpBackend }

object Interpreters {
  def liveAlgebra[F[_]: Monad](cache: Cache[F], oneFrame: OneFrame[F]): Algebra[F] =
    new LiveAlgebra[F](cache, oneFrame)

  def liveOneFrame[F[_]: Monad: Functor: Concurrent: Timer](
      config: OneFrameConfig
  )(implicit backend: SttpBackend[F, Nothing, NothingT]) =
    new LiveOneFrame[F](config)

  def liveOneFrameCacheUpdater[F[_]: Monad](cache: Cache[F], oneFrame: OneFrame[F]): LiveOneFrameCacheUpdater[F] =
    LiveOneFrameCacheUpdater.of(cache: Cache[F], oneFrame: OneFrame[F])
}

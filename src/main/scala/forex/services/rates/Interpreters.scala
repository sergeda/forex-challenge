package forex.services.rates

import cats.Monad
import forex.services.cache.Cache
import interpreters._

object Interpreters {
  def live[F[_]: Monad](cache: F[Cache[F]], oneFrame: OneFrame[F]): Algebra[F] =
    new LiveAlgebra[F](cache, oneFrame)
}

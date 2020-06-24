package forex.services.rates.interpreters

import cats.Monad
import cats.data.OptionT
import cats.syntax.either._
import cats.syntax.show._
import forex.domain.Rate
import forex.services.cache.Cache
import forex.services.rates.{ Algebra, OneFrame }
import forex.services.rates.errors._

class LiveAlgebra[F[_]: Monad](cache: Cache[F], oneFrame: OneFrame[F]) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    OptionT(cache.get(pair.show))
      .map(_.asRight[Error])
      .getOrElseF(oneFrame.get(pair))
}

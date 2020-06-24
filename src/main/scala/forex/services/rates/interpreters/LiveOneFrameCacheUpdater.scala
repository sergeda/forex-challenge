package forex.services.rates.interpreters

import cats.data.{ NonEmptyList, OptionT }
import cats.effect.{ Sync, Timer }
import cats.instances.list._
import cats.syntax.option._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import cats.{ Monad, Semigroupal, Traverse }
import forex.domain.{ Currency, Rate }
import forex.services.cache.Cache
import forex.services.rates.{ OneFrame, OneFrameCacheUpdater }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

class LiveOneFrameCacheUpdater[F[_]: Monad] private (cache: Cache[F], oneFrame: OneFrame[F])
    extends OneFrameCacheUpdater[F] {

  private def updateCache(rates: NonEmptyList[Rate]): F[NonEmptyList[Unit]] = {
    val updateResults: NonEmptyList[F[Unit]] = rates.map(rate => cache.put(rate.pair.show, rate))
    Traverse[NonEmptyList].sequence(updateResults)
  }

  private def getRates(pairs: List[Rate.Pair]): F[Option[NonEmptyList[Rate]]] =
    NonEmptyList.fromList(pairs) match {
      case Some(value) => oneFrame.get(value, true).map(_.toOption)
      case None        => Monad[F].pure(none[NonEmptyList[Rate]])
    }

  override def update(pairs: List[Rate.Pair]): F[Option[NonEmptyList[Unit]]] =
    (for {
      rates <- OptionT(getRates(pairs))
      result <- OptionT.liftF(updateCache(rates))
    } yield result).value

}

object LiveOneFrameCacheUpdater {
  def of[F[_]: Monad](cache: Cache[F], oneFrame: OneFrame[F]): LiveOneFrameCacheUpdater[F] =
    new LiveOneFrameCacheUpdater(cache, oneFrame)

  private def allPairs: List[Rate.Pair] =
    Semigroupal[List]
      .product(Currency.list, Currency.list)
      .filter(v => v._1 != v._2)
      .map(Rate.Pair.apply)

  def schedule[F[_]: Sync: Timer](period: FiniteDuration, cacheUpdater: OneFrameCacheUpdater[F]): F[Unit] =
    Timer[F].sleep(5.seconds) >> cacheUpdater.update(allPairs) >> Timer[F].sleep(period) >> schedule(period, cacheUpdater)

}

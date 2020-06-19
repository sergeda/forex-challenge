package forex.services.cache.interpreters

import cats.Monad
import cats.effect.{ Clock, Sync, Timer }
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.concurrent.Ref
import forex.config.CacheConfig
import forex.services.cache.Cache

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

final class LiveCache[F[_]: Monad: Clock, K, V] private (state: Ref[F, Map[K, (Long, V)]], expires: FiniteDuration)
    extends Cache[F, K, V] {
  override def get(key: K): F[Option[V]] = state.get.map(_.get(key).map(_._2))

  override def put(key: K, value: V): F[Unit] =
    for {
      currentTime <- Clock[F].realTime(MILLISECONDS)
      _ <- state.update(_.updated(key, (currentTime + expires.toMillis, value)))
    } yield ()
}

object LiveCache {
  private def cleanExpired[F[_]: Monad: Clock: Timer, K, V](clean: FiniteDuration,
                                                            state: Ref[F, Map[K, (Long, V)]]): F[Unit] =
    for {
      currentTime <- Clock[F].realTime(MILLISECONDS)
      _ <- state.update(_.filter { case (_, (expiration, _)) => expiration > currentTime })
      _ <- Timer[F].sleep(clean)
      _ <- cleanExpired(clean, state)
    } yield ()

  def of[F[_]: Sync: Clock: Timer, K, V](config: CacheConfig): F[LiveCache[F, K, V]] =
    for {
      ref <- Ref[F].of(Map.empty[K, (Long, V)])
      - <- cleanExpired(config.clean, ref)
    } yield new LiveCache(ref, config.expire)
}

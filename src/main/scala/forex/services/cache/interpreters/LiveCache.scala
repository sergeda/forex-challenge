package forex.services.cache.interpreters

import cats.Functor
import cats.effect.Async
import forex.domain.Rate
import forex.services.cache.Cache
import org.cache2k.Cache2kBuilder
import scalacache.{Mode, _}
import scalacache.cache2k.Cache2kCache

import scala.concurrent.duration.FiniteDuration

final class LiveCache[F[_]: Async: Functor] private(state: Cache2kCache[Rate])
    extends Cache[F] {

  implicit val mode: Mode[F] = scalacache.CatsEffect.modes.async[F]

  override def get(key: String): F[Option[Rate]] = state.get(key)

  override def put(key: String, value: Rate): F[Unit] = state.put(key)(value).asInstanceOf[F[Unit]]

}

object LiveCache {

  def of[F[_]: Async](expires: FiniteDuration): F[Cache[F]] = {
    val underlyingCache2kCache = new Cache2kBuilder[String, Rate]() {}.expireAfterWrite(expires._1, expires._2).build
    implicit val customisedCache2kCache: Cache2kCache[Rate] = Cache2kCache(underlyingCache2kCache)
    Async[F].delay(new LiveCache[F](customisedCache2kCache))
  }
}

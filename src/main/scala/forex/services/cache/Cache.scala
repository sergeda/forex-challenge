package forex.services.cache

import forex.domain.Rate

trait Cache[F[_]] {
  def get(key: String): F[Option[Rate]]
  def put(key: String, value: Rate): F[Unit]
}

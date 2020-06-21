package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    cache: CacheConfig,
    oneframe: OneFrameConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameConfig(
    token: String,
    url: String,
    timeout: FiniteDuration
)

case class CacheConfig(
    expire: FiniteDuration,
    clean: FiniteDuration
)

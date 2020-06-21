package forex

import cats.effect._
import cats.syntax.functor._
import forex.config._
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import sttp.client.{ NothingT, SttpBackend }
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    AsyncHttpClientCatsBackend.resource[IO]().use { implicit backend =>
      new Application[IO].stream.compile.drain.as(ExitCode.Success)
    }

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(implicit backend: SttpBackend[F, Nothing, NothingT]): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      module = new Module[F](config)
      _ <- BlazeServerBuilder[F]
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}

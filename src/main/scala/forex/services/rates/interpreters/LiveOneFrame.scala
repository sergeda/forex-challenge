package forex.services.rates.interpreters

import cats.Functor
import cats.data.{ EitherT, NonEmptyList }
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.services.rates.OneFrame
import forex.services.rates.errors._
import cats.syntax.show._
import cats.syntax.flatMap._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.foldable._
import cats.instances.either.catsStdInstancesForEither
import cats.instances.string.catsStdShowForString
import forex.services.rates.errors.Error.OneFrameLookupFailed
import io.circe.parser.decode
import sttp.client._
import sttp.client.{ asString, basicRequest, NothingT, SttpBackend }
import forex.services.rates.Protocol._
import forex.services.rates.Converters._

class LiveOneFrame[F[_]: Functor](config: OneFrameConfig)(implicit val backend: SttpBackend[F, Nothing, NothingT])
    extends OneFrame[F] {

  type ClientResponse = Response[Either[String, String]]

  private val url     = config.url
  private val token   = config.token
  private val timeout = config.timeout

  override def get(pair: Rate.Pair): F[Either[Error, Rate]] =
    EitherT(get(NonEmptyList(pair, List.empty))).map(_.head).value

  override def get(pairs: NonEmptyList[Rate.Pair]): F[Either[Error, NonEmptyList[Rate]]] =
    request(pairs).map(parseResponse)

  private def request(pairs: NonEmptyList[Rate.Pair]): F[ClientResponse] = {
    val requestQuery = pairs.map(_.show).mkString_("&")
    val fullUrl      = if (url.endsWith("?")) url + requestQuery else url + "?" + requestQuery
    basicRequest
      .get(uri"$fullUrl")
      .header("token", s"token $token", true)
      .response(asString("UTF-8"))
      .readTimeout(timeout)
      .send()
  }

  private def parseResponse(response: ClientResponse): Either[Error, NonEmptyList[Rate]] =
    if (response.code.isSuccess) {
      response.body
        .leftMap(error => OneFrameLookupFailed(error))
        .map { content =>
          decodeContent(content)
        }
        .flatten
    } else OneFrameLookupFailed("Non-200 response from OneFrame: " + response.statusText).asLeft[NonEmptyList[Rate]]

  private def decodeContent(content: String): Either[Error, NonEmptyList[Rate]] =
    decode[NonEmptyList[OneFrameResponse]](content)
      .leftMap(error => OneFrameLookupFailed(error.getMessage))
      .map(_.map(_.asRate))

}

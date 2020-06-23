package forex.services.rates.interpreters

import cats.data.{ EitherT, NonEmptyList }
import cats.effect.Concurrent
import cats.instances.either.catsStdInstancesForEither
import cats.instances.string.catsStdShowForString
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import cats.syntax.show._
import cats.{ Functor, Monad }
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.services.rates.Converters._
import forex.services.rates.OneFrame
import forex.services.rates.Protocol._
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.errors._
import io.circe.parser.decode
import sttp.client.{ NothingT, SttpBackend, asString, basicRequest, _ }

class LiveOneFrame[F[_]: Monad: Functor: Concurrent](config: OneFrameConfig)(
    implicit val backend: SttpBackend[F, Nothing, NothingT]
) extends OneFrame[F] {

  type ClientResponse = Response[Either[String, String]]

  private val url     = config.url
  private val token   = config.token
  private val timeout = config.timeout

  override def get(pair: Rate.Pair): F[Either[Error, Rate]] =
    EitherT(get(NonEmptyList(pair, List.empty))).map(_.head).value

  override def get(pairs: NonEmptyList[Rate.Pair]): F[Either[Error, NonEmptyList[Rate]]] =
    recoverResponse(request(pairs)).map(parseResponse)

  private def formUrl(pairs: NonEmptyList[Rate.Pair]): String = {
    val requestQuery = pairs.map(_.show).mkString_("pair=", "&pair=", "")
    if (url.endsWith("?")) url + requestQuery else url + "?" + requestQuery
  }

  private def request(pairs: NonEmptyList[Rate.Pair]): F[ClientResponse] =
    basicRequest
      .get(uri"${formUrl(pairs)}")
      .header("token", s"token $token", true)
      .response(asString("UTF-8"))
      .readTimeout(timeout)
      .send()

  private def recoverResponse(response: F[ClientResponse]): F[Either[String, String]] =
    response.map(_.body).attempt.map(_.leftMap(_.getMessage).flatten)

  private def parseResponse(response: Either[String, String]): Either[Error, NonEmptyList[Rate]] =
    response.leftMap(error => OneFrameLookupFailed(error)).map { decodeContent }.flatten

  private def decodeContent(content: String): Either[Error, NonEmptyList[Rate]] =
    decode[NonEmptyList[OneFrameResponse]](content)
      .leftMap(_ => OneFrameLookupFailed("Incorrect response from OneFrame: " + content))
      .map(_.map(_.asRate))

}

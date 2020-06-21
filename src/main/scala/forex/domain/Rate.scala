package forex.domain

import cats.syntax.show._
import cats.Show

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    implicit def show: Show[Pair] = Show.show(pair => pair.from.show + pair.to.show)
  }
}

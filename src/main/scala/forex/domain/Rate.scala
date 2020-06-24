package forex.domain

import cats.Show
import cats.syntax.show._

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
    def apply(currencies: (Currency, Currency)): Pair = Pair(currencies._1, currencies._2)
  }
}

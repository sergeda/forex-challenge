package forex

package object services {
  type RatesService[F[_]]    = rates.Algebra[F]
  type OneFrameService[F[_]] = rates.OneFrame[F]
  final val RatesServices = rates.Interpreters
}

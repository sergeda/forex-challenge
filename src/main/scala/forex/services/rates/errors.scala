package forex.services.rates

object errors {

  sealed trait Error {
    def msg: String
  }
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
  }

}

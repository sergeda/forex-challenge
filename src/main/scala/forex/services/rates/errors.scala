package forex.services.rates

object errors {

  sealed trait Error {
    def msg: String
  }
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    object OneFrameLookupFailed {
      def apply(throwable: Throwable): OneFrameLookupFailed = {
        val msg = Option(throwable.getMessage).getOrElse("Unknown error")
        new OneFrameLookupFailed(msg)
      }
    }
  }

}

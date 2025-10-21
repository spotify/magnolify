package magnolify.bigquery

case class BigNumeric private (wkt: BigDecimal)
object BigNumeric {
  val MaxNumericPrecision = 77
  val MaxNumericScale = 38

  def apply(value: String): BigNumeric = new BigNumeric(BigDecimal(value))
}

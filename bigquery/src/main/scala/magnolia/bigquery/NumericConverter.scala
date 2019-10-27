package magnolia.bigquery

import java.math.MathContext

private object NumericConverter {
  def toBigDecimal(v: Any): BigDecimal = scale(BigDecimal(v.toString))
  def fromBigDecimal(v: BigDecimal): Any = scale(v).toString()

  private val MaxNumericPrecision = 38
  private val MaxNumericScale = 9

  private def scale(value: BigDecimal): BigDecimal = {
    // NUMERIC's max scale is 9, precision is 38
    val scaled = if (value.scale > MaxNumericScale) {
      value.setScale(MaxNumericScale, scala.math.BigDecimal.RoundingMode.HALF_UP)
    } else {
      value
    }
    require(
      scaled.precision <= MaxNumericPrecision,
      s"max allowed precision is $MaxNumericPrecision"
    )
    BigDecimal(scaled.toString, new MathContext(MaxNumericPrecision))
  }
}
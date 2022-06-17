package magnolify.bigquery.unsafe

import magnolify.bigquery.TableRowField

trait BigQueryUnsafeImplicits:
  given TableRowField[Byte] = TableRowField.from[Long](_.toByte)(_.toLong)(TableRowField.trfLong)
  given TableRowField[Char] = TableRowField.from[Long](_.toChar)(_.toLong)(TableRowField.trfLong)
  given TableRowField[Short] = TableRowField.from[Long](_.toShort)(_.toLong)(TableRowField.trfLong)
  given TableRowField[Int] = TableRowField.from[Long](_.toInt)(_.toLong)(TableRowField.trfLong)
  given TableRowField[Float] =
    TableRowField.from[Double](_.toFloat)(_.toDouble)(TableRowField.trfDouble)

object BigQueryUnsafeImplicits extends BigQueryUnsafeImplicits

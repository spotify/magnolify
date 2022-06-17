package magnolify.bigquery.unsafe

import magnolify.bigquery.TableRowField

trait BigQueryUnsafeImplicits {
  implicit val trfByte: TableRowField[Byte] =
    TableRowField.from[Long](_.toByte)(_.toLong)(TableRowField.trfLong)
  implicit val trfChar: TableRowField[Char] =
    TableRowField.from[Long](_.toChar)(_.toLong)(TableRowField.trfLong)
  implicit val trfShort: TableRowField[Short] =
    TableRowField.from[Long](_.toShort)(_.toLong)(TableRowField.trfLong)
  implicit val trfInt: TableRowField[Int] =
    TableRowField.from[Long](_.toInt)(_.toLong)(TableRowField.trfLong)
  implicit val trfFloat: TableRowField[Float] =
    TableRowField.from[Double](_.toFloat)(_.toDouble)(TableRowField.trfDouble)
}

object BigQueryUnsafeImplicits extends BigQueryUnsafeImplicits

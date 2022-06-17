package magnolify.bigquery

import java.time._

trait BigQueryImplicits:
  given [T: TableRowField.Record]: TableRowType[T] = TableRowType[T]

  given TableRowField[Boolean] = TableRowField.trfBool
  given TableRowField[Long] = TableRowField.trfLong
  given TableRowField[Double] = TableRowField.trfDouble
  given TableRowField[String] = TableRowField.trfString
  given TableRowField[BigDecimal] = TableRowField.trfNumeric
  given TableRowField[Array[Byte]] = TableRowField.trfByteArray
  given TableRowField[Instant] = TableRowField.trfInstant
  given TableRowField[LocalDate] = TableRowField.trfDate
  given TableRowField[LocalTime] = TableRowField.trfTime
  given TableRowField[LocalDateTime] = TableRowField.trfDateTime

  given [T: TableRowField]: TableRowField[Option[T]] = TableRowField.trfOption

object BigQueryImplicits extends BigQueryImplicits

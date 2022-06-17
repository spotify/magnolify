package magnolify.avro.logical

import magnolify.avro.AvroField
import org.apache.avro.LogicalTypes.LogicalTypeFactory
import org.apache.avro.{LogicalType, LogicalTypes, Schema}

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.{Instant, LocalDateTime, LocalTime, ZoneOffset}

object AvroTimeMicros {
  val afTimestampMicros: AvroField[Instant] =
    AvroField.logicalType[Long](LogicalTypes.timestampMicros())(us =>
      Instant.ofEpochMilli(us / 1000)
    )(_.toEpochMilli * 1000)(AvroField.afLong)

  val afTimeMicros: AvroField[LocalTime] =
    AvroField.logicalType[Long](LogicalTypes.timeMicros())(us => LocalTime.ofNanoOfDay(us * 1000))(
      _.toNanoOfDay / 1000
    )(AvroField.afLong)

  // `LogicalTypes.localTimestampMicros` is Avro 1.10.0+
  val afLocalTimestampMicros: AvroField[LocalDateTime] =
    AvroField.logicalType[Long](new LogicalType("local-timestamp-micros"))(us =>
      LocalDateTime.ofInstant(Instant.ofEpochMilli(us / 1000), ZoneOffset.UTC)
    )(_.toInstant(ZoneOffset.UTC).toEpochMilli * 1000)(AvroField.afLong)
}

object AvroTimeMillis {
  val afTimestampMillis: AvroField[Instant] =
    AvroField.logicalType[Long](LogicalTypes.timestampMillis())(Instant.ofEpochMilli)(
      _.toEpochMilli
    )(AvroField.afLong)

  val afTimeMillis: AvroField[LocalTime] =
    AvroField.logicalType[Int](LogicalTypes.timeMillis())(ms =>
      LocalTime.ofNanoOfDay(ms * 1000000L)
    )(t => (t.toNanoOfDay / 1000000).toInt)(AvroField.afInt)

  // `LogicalTypes.localTimestampMillis` is Avro 1.10.0+
  val afLocalTimestampMillis: AvroField[LocalDateTime] =
    AvroField.logicalType[Long](new LogicalType("local-timestamp-millis"))(ms =>
      LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneOffset.UTC)
    )(_.toInstant(ZoneOffset.UTC).toEpochMilli)(AvroField.afLong)
}

object AvroBigQuery {
  // datetime is a custom logical type and must be registered
  private final val DateTimeTypeName = "datetime"
  private final val DateTimeLogicalTypeFactory: LogicalTypeFactory = (schema: Schema) =>
    new org.apache.avro.LogicalType(DateTimeTypeName)

  /**
   * Register custom logical types with avro, which is necessary to correctly parse a custom logical
   * type from string. If registration is omitted, the returned string schema will be correct, but
   * the logicalType field will be null. The registry is global mutable state, keyed on the type
   * name.
   */
  def registerLogicalTypes(): Unit =
    org.apache.avro.LogicalTypes.register(DateTimeTypeName, DateTimeLogicalTypeFactory)

  // DATETIME
  // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]]
  private val DatetimePrinter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
  private val DatetimeParser = new DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    .appendOptional(
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern(" HH:mm:ss"))
        .appendOptional(DateTimeFormatter.ofPattern(".SSSSSS"))
        .toFormatter
    )
    .toFormatter
    .withZone(ZoneOffset.UTC)

  // NUMERIC
  // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type
  val afBigQueryNumeric: AvroField[BigDecimal] = AvroField.bigDecimal(38, 9)

  // TIMESTAMP
  val afBigQueryTimestamp: AvroField[Instant] = AvroTimeMicros.afTimestampMicros

  // DATE: `AvroField.afDate`

  // TIME
  val afBigQueryTime: AvroField[LocalTime] = AvroTimeMicros.afTimeMicros

  // DATETIME -> sqlType: DATETIME
  val afBigQueryDatetime: AvroField[LocalDateTime] =
    AvroField.logicalType[String](new org.apache.avro.LogicalType(DateTimeTypeName))(s =>
      LocalDateTime.from(DatetimeParser.parse(s))
    )(DatetimePrinter.format)(AvroField.afString)
}

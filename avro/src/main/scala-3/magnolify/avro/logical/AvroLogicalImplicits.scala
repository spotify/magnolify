package magnolify.avro.logical

import magnolify.avro.AvroField
import org.apache.avro.LogicalTypes.LogicalTypeFactory
import org.apache.avro.{LogicalType, LogicalTypes, Schema}

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.{Instant, LocalDateTime, LocalTime, ZoneOffset}

trait AvroLogicalImplicits

trait AvroTimeMicrosImplicits {
  given AvroField[Instant] = AvroTimeMicros.afTimestampMicros
  given AvroField[LocalTime] = AvroTimeMicros.afTimeMicros
  given AvroField[LocalDateTime] = AvroTimeMicros.afLocalTimestampMicros
}

object AvroTimeMicrosImplicits extends AvroTimeMicrosImplicits

trait AvroTimeMillisImplicits {
  given AvroField[Instant] = AvroTimeMillis.afTimestampMillis
  given AvroField[LocalTime] = AvroTimeMillis.afTimeMillis
  given AvroField[LocalDateTime] = AvroTimeMillis.afLocalTimestampMillis
}

object AvroTimeMillisImplicits extends AvroTimeMillisImplicits

trait AvroBigQueryImplicits {
  given AvroField[BigDecimal] = AvroBigQuery.afBigQueryNumeric
  given AvroField[Instant] = AvroBigQuery.afBigQueryTimestamp
  given AvroField[LocalTime] = AvroBigQuery.afBigQueryTime
  given AvroField[LocalDateTime] = AvroBigQuery.afBigQueryDatetime
}

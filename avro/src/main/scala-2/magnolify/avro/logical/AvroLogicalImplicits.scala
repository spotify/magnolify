package magnolify.avro.logical

import magnolify.avro.AvroField
import org.apache.avro.LogicalTypes.LogicalTypeFactory
import org.apache.avro.{LogicalType, LogicalTypes, Schema}

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.{Instant, LocalDateTime, LocalTime, ZoneOffset}

trait AvroLogicalImplicits

trait AvroTimeMicrosImplicits {
  implicit val afTimestampMicros: AvroField[Instant] = AvroTimeMicros.afTimestampMicros
  implicit val afTimeMicros: AvroField[LocalTime] = AvroTimeMicros.afTimeMicros
  implicit val afLocalTimestampMicros: AvroField[LocalDateTime] =
    AvroTimeMicros.afLocalTimestampMicros
}

object AvroTimeMicrosImplicits extends AvroTimeMicrosImplicits

trait AvroTimeMillisImplicits {
  implicit val afTimestampMillis: AvroField[Instant] = AvroTimeMillis.afTimestampMillis
  implicit val afTimeMillis: AvroField[LocalTime] = AvroTimeMillis.afTimeMillis
  implicit val afLocalTimestampMillis: AvroField[LocalDateTime] =
    AvroTimeMillis.afLocalTimestampMillis
}

object AvroTimeMillisImplicits extends AvroTimeMillisImplicits

trait AvroBigQueryImplicits {
  implicit val afBigQueryNumeric: AvroField[BigDecimal] = AvroBigQuery.afBigQueryNumeric
  implicit val afBigQueryTimestamp: AvroField[Instant] = AvroBigQuery.afBigQueryTimestamp
  implicit val afBigQueryTime: AvroField[LocalTime] = AvroBigQuery.afBigQueryTime
  implicit val afBigQueryDatetime: AvroField[LocalDateTime] = AvroBigQuery.afBigQueryDatetime
}

object AvroBigQueryImplicits extends AvroBigQueryImplicits

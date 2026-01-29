/*
 * Copyright 2020 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package magnolify.avro

import org.apache.avro.LogicalTypes.LogicalTypeFactory
import org.apache.avro.{LogicalType, LogicalTypes, Schema}
import org.joda.{time => joda}

import java.time._
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}

package object logical {
  import magnolify.shared.Time._
  // Duplicate implementation from org.apache.avro.data.TimeConversions
  // to support both 1.8 (joda-time based) and 1.9+ (java-time based)
  object micros {
    implicit val afTimestampMicros: AvroField[Instant] =
      AvroField.logicalType[Long](LogicalTypes.timestampMicros())(microsToInstant)(
        microsFromInstant
      )
    implicit val afJodaInstantMicros: AvroField[joda.Instant] =
      AvroField.logicalType[Long](LogicalTypes.timestampMicros())(microsToJodaInstant)(
        microsFromJodaInstant
      )

    implicit val afLocalTimeMicros: AvroField[LocalTime] =
      AvroField.logicalType[Long](LogicalTypes.timeMicros())(microsToLocalTime)(microsFromLocalTime)
    implicit val afJodaLocalTimeMicros: AvroField[joda.LocalTime] =
      AvroField.logicalType[Long](LogicalTypes.timeMicros())(microsToJodaLocalTime)(
        microsFromJodaLocalTime
      )

    // `LogicalTypes.localTimestampMicros()` is Avro 1.10
    implicit val afLocalDateTimeMicros: AvroField[LocalDateTime] =
      AvroField.logicalType[Long](new LogicalType("local-timestamp-micros"))(microsToLocalDateTime)(
        microsFromLocalDateTime
      )
    implicit val afJodaLocalDateTimeMicros: AvroField[joda.LocalDateTime] =
      AvroField.logicalType[Long](new LogicalType("local-timestamp-micros"))(
        microsToJodaLocalDateTime
      )(microsFromJodaLocalDateTime)

    // avro 1.8 uses joda-time
    implicit val afJodaDateTimeMicros: AvroField[joda.DateTime] =
      AvroField.logicalType[Long](LogicalTypes.timestampMicros())(microsToJodaDateTime)(
        microsFromJodaDateTime
      )

    // Deprecated aliases for backward compatibility
    @deprecated("Use afLocalTimeMicros instead", "0.9.0")
    val afTimeMicros: AvroField[LocalTime] = afLocalTimeMicros

    @deprecated("Use afJodaLocalTimeMicros instead", "0.9.0")
    val afJodaTimeMicros: AvroField[joda.LocalTime] = afJodaLocalTimeMicros

    @deprecated("Use afLocalDateTimeMicros instead", "0.9.0")
    val afLocalTimestampMicros: AvroField[LocalDateTime] = afLocalDateTimeMicros

    @deprecated("Use afJodaDateTimeMicros instead", "0.9.0")
    val afJodaTimestampMicros: AvroField[joda.DateTime] = afJodaDateTimeMicros
  }

  object millis {
    implicit val afInstantMillis: AvroField[Instant] =
      AvroField.logicalType[Long](LogicalTypes.timestampMillis())(millisToInstant)(
        millisFromInstant
      )
    implicit val afJodaInstantMillis: AvroField[joda.Instant] =
      AvroField.logicalType[Long](LogicalTypes.timestampMillis())(millisToJodaInstant)(
        millisFromJodaInstant
      )

    implicit val afLocalTimeMillis: AvroField[LocalTime] =
      AvroField.logicalType[Int](LogicalTypes.timeMillis())(millisToLocalTime)(millisFromLocalTime)
    implicit val afJodaLocalTimeMillis: AvroField[joda.LocalTime] =
      AvroField.logicalType[Int](LogicalTypes.timeMillis())(millisToJodaLocalTime)(
        millisFromJodaLocalTime
      )

    // `LogicalTypes.localTimestampMillis` is Avro 1.10.0+
    implicit val afLocalDateTimeMillis: AvroField[LocalDateTime] =
      AvroField.logicalType[Long](new LogicalType("local-timestamp-millis"))(millisToLocalDateTime)(
        millisFromLocalDateTime
      )
    implicit val afJodaLocalDateTimeMillis: AvroField[joda.LocalDateTime] =
      AvroField.logicalType[Long](new LogicalType("local-timestamp-millis"))(
        millisToJodaLocalDateTime
      )(millisFromJodaLocalDateTime)

    // avro 1.8 uses joda-time
    implicit val afJodaDateTimeMillis: AvroField[joda.DateTime] =
      AvroField.logicalType[Long](LogicalTypes.timestampMillis())(millisToJodaDateTime)(
        millisFromJodaDateTime
      )

    // Deprecated aliases for backward compatibility
    @deprecated("Use afInstantMillis instead", "0.9.0")
    val afTimestampMillis: AvroField[Instant] = afInstantMillis

    @deprecated("Use afLocalTimeMillis instead", "0.9.0")
    val afTimeMillis: AvroField[LocalTime] = afLocalTimeMillis

    @deprecated("Use afJodaLocalTimeMillis instead", "0.9.0")
    val afJodaTimeMillis: AvroField[joda.LocalTime] = afJodaLocalTimeMillis

    @deprecated("Use afLocalDateTimeMillis instead", "0.9.0")
    val afLocalTimestampMillis: AvroField[LocalDateTime] = afLocalDateTimeMillis

    @deprecated("Use afJodaDateTimeMillis instead", "0.9.0")
    val afJodaTimestampMillis: AvroField[joda.DateTime] = afJodaDateTimeMillis
  }

  object bigquery {
    // datetime is a custom logical type and must be registered
    private final val DateTimeTypeName = "datetime"
    private final val DateTimeLogicalTypeFactory: LogicalTypeFactory = (_: Schema) =>
      new LogicalType(DateTimeTypeName)

    /**
     * Register custom logical types with avro, which is necessary to correctly parse a custom
     * logical type from string. If registration is omitted, the returned string schema will be
     * correct, but the logicalType field will be null. The registry is global mutable state, keyed
     * on the type name.
     */
    def registerLogicalTypes(): Unit =
      LogicalTypes.register(DateTimeTypeName, DateTimeLogicalTypeFactory)

    // DATETIME
    // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]]
    private val DatePattern = "yyyy-MM-dd"
    private val TimePattern = "HH:mm:ss"
    private val DecimalPattern = "SSSSSS"
    private val DatetimePattern = s"$DatePattern $TimePattern.$DecimalPattern"
    private val DatetimePrinter = DateTimeFormatter.ofPattern(DatetimePattern)
    private val DatetimeParser = new DateTimeFormatterBuilder()
      .appendPattern(DatePattern)
      .appendOptional(
        new DateTimeFormatterBuilder()
          .appendLiteral(' ')
          .append(new DateTimeFormatterBuilder().appendPattern(TimePattern).toFormatter)
          .appendOptional(
            new DateTimeFormatterBuilder()
              .appendLiteral('.')
              .appendPattern(DecimalPattern)
              .toFormatter
          )
          .toFormatter
      )
      .toFormatter
      .withZone(ZoneOffset.UTC)

    private val JodaDatetimePrinter = new joda.format.DateTimeFormatterBuilder()
      .appendPattern(DatetimePattern)
      .toFormatter

    private val JodaDatetimeParser = new joda.format.DateTimeFormatterBuilder()
      .appendPattern(DatePattern)
      .appendOptional(
        new joda.format.DateTimeFormatterBuilder()
          .appendLiteral(' ')
          .appendPattern(TimePattern)
          .appendOptional(
            new joda.format.DateTimeFormatterBuilder()
              .appendLiteral('.')
              .appendPattern(DecimalPattern)
              .toParser
          )
          .toParser
      )
      .toFormatter
      .withZone(joda.DateTimeZone.UTC)

    // NUMERIC
    // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type
    implicit val afBigQueryNumeric: AvroField[BigDecimal] = AvroField.bigDecimal(38, 9)

    // TIMESTAMP
    implicit val afBigQueryTimestamp: AvroField[Instant] = micros.afTimestampMicros
    implicit val afBigQueryJodaTimestamp: AvroField[joda.DateTime] =
      micros.afJodaDateTimeMicros

    // DATE: `AvroField.afDate`

    // TIME
    implicit val afBigQueryTime: AvroField[LocalTime] = micros.afLocalTimeMicros
    implicit val afBigQueryJodaTime: AvroField[joda.LocalTime] = micros.afJodaLocalTimeMicros

    // DATETIME -> sqlType: DATETIME
    implicit val afBigQueryDatetime: AvroField[LocalDateTime] =
      AvroField.logicalType[CharSequence](new org.apache.avro.LogicalType(DateTimeTypeName)) { cs =>
        LocalDateTime.parse(cs.toString, DatetimeParser)
      } { datetime =>
        DatetimePrinter.format(datetime)
      }
    implicit val afBigQueryJodaDatetime: AvroField[joda.LocalDateTime] =
      AvroField.logicalType[CharSequence](new org.apache.avro.LogicalType(DateTimeTypeName)) { cs =>
        joda.LocalDateTime.parse(cs.toString, JodaDatetimeParser)
      } { datetime =>
        JodaDatetimePrinter.print(datetime)
      }
  }
}

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
import java.util.concurrent.TimeUnit

package object logical {
  // Duplicate implementation from org.apache.avro.data.TimeConversions
  // to support both 1.8 (joda-time based) and 1.9+ (java-time based)
  object micros {
    private def toTimestampMicros(microsFromEpoch: Long): Instant = {
      val epochSeconds = microsFromEpoch / 1000000L
      val nanoAdjustment = (microsFromEpoch % 1000000L) * 1000L;
      Instant.ofEpochSecond(epochSeconds, nanoAdjustment)
    }

    private def fromTimestampMicros(instant: Instant): Long = {
      val seconds = instant.getEpochSecond
      val nanos = instant.getNano
      if (seconds < 0 && nanos > 0) {
        val micros = Math.multiplyExact(seconds + 1, 1000000L)
        val adjustment = (nanos / 1000L) - 1000000
        Math.addExact(micros, adjustment)
      } else {
        val micros = Math.multiplyExact(seconds, 1000000L)
        Math.addExact(micros, nanos / 1000L)
      }
    }

    implicit val afTimestampMicros: AvroField[Instant] =
      AvroField.logicalType[Long](LogicalTypes.timestampMicros())(toTimestampMicros)(
        fromTimestampMicros
      )

    implicit val afTimeMicros: AvroField[LocalTime] =
      AvroField.logicalType[Long](LogicalTypes.timeMicros()) { us =>
        LocalTime.ofNanoOfDay(TimeUnit.MICROSECONDS.toNanos(us))
      } { time =>
        TimeUnit.NANOSECONDS.toMicros(time.toNanoOfDay)
      }

    // `LogicalTypes.localTimestampMicros()` is Avro 1.10
    implicit val afLocalTimestampMicros: AvroField[LocalDateTime] =
      AvroField.logicalType[Long](new LogicalType("local-timestamp-micros")) { microsFromEpoch =>
        val instant = toTimestampMicros(microsFromEpoch)
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
      } { timestamp =>
        val instant = timestamp.toInstant(ZoneOffset.UTC)
        fromTimestampMicros(instant)
      }

    // avro 1.8 uses joda-time
    implicit val afJodaTimestampMicros: AvroField[joda.DateTime] =
      AvroField.logicalType[Long](LogicalTypes.timestampMicros()) { microsFromEpoch =>
        new joda.DateTime(microsFromEpoch / 1000, joda.DateTimeZone.UTC)
      } { timestamp =>
        1000 * timestamp.getMillis
      }

    implicit val afJodaTimeMicros: AvroField[joda.LocalTime] =
      AvroField.logicalType[Long](LogicalTypes.timeMicros()) { microsFromMidnight =>
        joda.LocalTime.fromMillisOfDay(microsFromMidnight / 1000)
      } { time =>
        // from LossyTimeMicrosConversion
        1000L * time.millisOfDay().get()
      }
  }

  object millis {
    implicit val afTimestampMillis: AvroField[Instant] =
      AvroField.logicalType[Long](LogicalTypes.timestampMillis()) { millisFromEpoch =>
        Instant.ofEpochMilli(millisFromEpoch)
      } { timestamp =>
        timestamp.toEpochMilli
      }

    implicit val afTimeMillis: AvroField[LocalTime] =
      AvroField.logicalType[Int](LogicalTypes.timeMillis()) { millisFromMidnight =>
        LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(millisFromMidnight.toLong))
      } { time =>
        TimeUnit.NANOSECONDS.toMillis(time.toNanoOfDay).toInt
      }

    // `LogicalTypes.localTimestampMillis` is Avro 1.10.0+
    implicit val afLocalTimestampMillis: AvroField[LocalDateTime] =
      AvroField.logicalType[Long](new LogicalType("local-timestamp-millis")) { millisFromEpoch =>
        val instant = Instant.ofEpochMilli(millisFromEpoch)
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
      } { timestamp =>
        val instant = timestamp.toInstant(ZoneOffset.UTC)
        instant.toEpochMilli
      }

    // avro 1.8 uses joda-time
    implicit val afJodaTimestampMillis: AvroField[joda.DateTime] =
      AvroField.logicalType[Long](LogicalTypes.timestampMillis()) { millisFromEpoch =>
        new joda.DateTime(millisFromEpoch, joda.DateTimeZone.UTC)
      } { timestamp =>
        timestamp.getMillis
      }

    implicit val afJodaTimeMillis: AvroField[joda.LocalTime] =
      AvroField.logicalType[Int](LogicalTypes.timeMillis()) { millisFromMidnight =>
        joda.LocalTime.fromMillisOfDay(millisFromMidnight.toLong)
      } { time =>
        time.millisOfDay().get()
      }
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
      micros.afJodaTimestampMicros

    // DATE: `AvroField.afDate`

    // TIME
    implicit val afBigQueryTime: AvroField[LocalTime] = micros.afTimeMicros
    implicit val afBigQueryJodaTime: AvroField[joda.LocalTime] = micros.afJodaTimeMicros

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

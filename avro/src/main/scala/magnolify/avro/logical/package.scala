/*
 * Copyright 2020 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.avro

import java.time.{Instant, LocalDateTime, LocalTime, ZoneOffset}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}

import org.apache.avro.{LogicalType, LogicalTypes}

package object logical {
  object micros {
    implicit val afTimestampMicros: AvroField[Instant] =
      AvroField.logicalType[Long](LogicalTypes.timestampMicros())(us =>
        Instant.ofEpochMilli(us / 1000)
      )(_.toEpochMilli * 1000)

    implicit val afTimeMicros: AvroField[LocalTime] =
      AvroField.logicalType[Long](LogicalTypes.timeMicros())(us =>
        LocalTime.ofNanoOfDay(us * 1000)
      )(_.toNanoOfDay / 1000)

    // `LogicalTypes.localTimestampMicros` is Avro 1.10.0+
    implicit val afLocalTimestampMicros: AvroField[LocalDateTime] =
      AvroField.logicalType[Long](new LogicalType("local-timestamp-micros"))(us =>
        LocalDateTime.ofInstant(Instant.ofEpochMilli(us / 1000), ZoneOffset.UTC)
      )(_.toInstant(ZoneOffset.UTC).toEpochMilli * 1000)
  }

  object millis {
    implicit val afTimestampMillis: AvroField[Instant] =
      AvroField.logicalType[Long](LogicalTypes.timestampMillis())(Instant.ofEpochMilli)(
        _.toEpochMilli
      )

    implicit val afTimeMillis: AvroField[LocalTime] =
      AvroField.logicalType[Int](LogicalTypes.timeMillis())(ms =>
        LocalTime.ofNanoOfDay(ms * 1000000L)
      )(t => (t.toNanoOfDay / 1000000).toInt)

    // `LogicalTypes.localTimestampMillis` is Avro 1.10.0+
    implicit val afLocalTimestampMillis: AvroField[LocalDateTime] =
      AvroField.logicalType[Long](new LogicalType("local-timestamp-micros"))(ms =>
        LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneOffset.UTC)
      )(_.toInstant(ZoneOffset.UTC).toEpochMilli)
  }

  object bigquery {
    // NUMERIC
    // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type
    implicit val afBigQueryNumeric: AvroField[BigDecimal] = AvroField.bigDecimal(38, 9)

    // TIMESTAMP
    implicit val afBigQueryTimestamp: AvroField[Instant] = micros.afTimestampMicros

    // DATE: `AvroField.afDate`

    // TIME
    implicit val afBigQueryTime: AvroField[LocalTime] = micros.afTimeMicros

    // DATETIME -> sqlType: DATETIME
    implicit val afBigQueryDatetime: AvroField[LocalDateTime] =
      AvroField.logicalType[String](new org.apache.avro.LogicalType("datetime"))(s =>
        LocalDateTime.from(datetimeParser.parse(s))
      )(datetimePrinter.format)

    // DATETIME
    // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]]
    private val datetimePrinter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
    private val datetimeParser = new DateTimeFormatterBuilder()
      .append(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
      .appendOptional(
        new DateTimeFormatterBuilder()
          .append(DateTimeFormatter.ofPattern(" HH:mm:ss"))
          .appendOptional(DateTimeFormatter.ofPattern(".SSSSSS"))
          .toFormatter
      )
      .toFormatter
      .withZone(ZoneOffset.UTC)
  }
}

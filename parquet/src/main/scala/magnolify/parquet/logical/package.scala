/*
 * Copyright 2021 Spotify AB.
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
package magnolify.parquet

import java.time._

import magnolify.parquet.ParquetField.Primitive
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit

package object logical {
  // TIME (millis i32, micros i64, nanos, i64), UTC true/false
  // TIMESTAMP (millis, micros, nanos), UTC true/false

  object millis {
    private val unit = TimeUnit.MILLIS

    // TIMESTAMP
    implicit val pfTimestampMillis: Primitive[Instant] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timestampType(true, unit))(
        Instant.ofEpochMilli
      )(_.toEpochMilli)
    implicit val pfLocalDateTimeMillis: Primitive[LocalDateTime] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timestampType(false, unit))(ms =>
        LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneOffset.UTC)
      )(
        _.toInstant(ZoneOffset.UTC).toEpochMilli
      )

    // TIME
    implicit val pfOffsetTimeMillis: Primitive[OffsetTime] =
      ParquetField.logicalType[Int](LogicalTypeAnnotation.timeType(true, unit))(ms =>
        LocalTime.ofNanoOfDay(ms * 1000000L).atOffset(ZoneOffset.UTC)
      )(t => (t.toLocalTime.toNanoOfDay / 1000000).toInt)
    implicit val pfLocalTimeMillis: Primitive[LocalTime] =
      ParquetField.logicalType[Int](LogicalTypeAnnotation.timeType(false, unit))(ms =>
        LocalTime.ofNanoOfDay(ms * 1000000L)
      )(t => (t.toNanoOfDay / 1000000).toInt)
  }

  object micros {
    private val unit = TimeUnit.MICROS

    // TIMESTAMP
    implicit val pfTimestampMillis: Primitive[Instant] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timestampType(true, unit))(us =>
        Instant.ofEpochMilli(us / 1000)
      )(_.toEpochMilli * 1000)
    implicit val pfLocalDateTimeMillis: Primitive[LocalDateTime] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timestampType(false, unit))(us =>
        LocalDateTime.ofInstant(Instant.ofEpochMilli(us / 1000), ZoneOffset.UTC)
      )(
        _.toInstant(ZoneOffset.UTC).toEpochMilli * 1000
      )

    // TIME
    implicit val pfOffsetTimeMicros: Primitive[OffsetTime] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timeType(true, unit))(us =>
        LocalTime.ofNanoOfDay(us * 1000).atOffset(ZoneOffset.UTC)
      )(_.toLocalTime.toNanoOfDay / 1000)
    implicit val pfLocalTimeMicros: Primitive[LocalTime] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timeType(false, unit))(us =>
        LocalTime.ofNanoOfDay(us * 1000)
      )(_.toNanoOfDay / 1000)
  }

  object nanos {
    private val unit = TimeUnit.NANOS

    // TIMESTAMP
    implicit val pfTimestampMillis: Primitive[Instant] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timestampType(true, unit))(ns =>
        Instant.ofEpochMilli(ns / 1000000)
      )(_.toEpochMilli * 1000000)
    implicit val pfLocalDateTimeMillis: Primitive[LocalDateTime] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timestampType(false, unit))(ns =>
        LocalDateTime.ofInstant(Instant.ofEpochMilli(ns / 1000000), ZoneOffset.UTC)
      )(
        _.toInstant(ZoneOffset.UTC).toEpochMilli * 1000000
      )

    // TIME
    implicit val pfOffsetTimeNanos: Primitive[OffsetTime] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timeType(true, unit))(ns =>
        LocalTime.ofNanoOfDay(ns).atOffset(ZoneOffset.UTC)
      )(_.toLocalTime.toNanoOfDay)
    implicit val pfLocalTimeNanos: Primitive[LocalTime] =
      ParquetField.logicalType[Long](LogicalTypeAnnotation.timeType(false, unit))(
        LocalTime.ofNanoOfDay
      )(_.toNanoOfDay)
  }
}

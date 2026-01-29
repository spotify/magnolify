/*
 * Copyright 2021 Spotify AB
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

package magnolify.parquet

import java.time._

import magnolify.parquet.ParquetField.Primitive
import org.joda.time as joda
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit

package object logical {
  import magnolify.shared.Time._
  // TIME (millis i32, micros i64, nanos, i64), UTC true/false
  // TIMESTAMP (millis, micros, nanos), UTC true/false

  object millis extends TimeTypes {
    protected val unit = TimeUnit.MILLIS

    // TIMESTAMP
    implicit val pfInstantMillis: Primitive[Instant] =
      ParquetField.logicalType[Long](ts(true))(millisToInstant)(millisFromInstant)
    implicit val pfJodaInstantMillis: Primitive[joda.Instant] =
      ParquetField.logicalType[Long](ts(true))(millisToJodaInstant)(millisFromJodaInstant)
    implicit val pfLocalDateTimeMillis: Primitive[LocalDateTime] =
      ParquetField.logicalType[Long](ts(false))(millisToLocalDateTime)(millisFromLocalDateTime)
    implicit val pfJodaLocalDateTimeMillis: Primitive[joda.LocalDateTime] =
      ParquetField.logicalType[Long](ts(false))(millisToJodaLocalDateTime)(
        millisFromJodaLocalDateTime
      )

    // TIME
    implicit val pfOffsetTimeMillis: Primitive[OffsetTime] =
      ParquetField.logicalType[Int](time(true))(ms =>
        millisToLocalTime(ms).atOffset(ZoneOffset.UTC)
      )(t => millisFromLocalTime(t.toLocalTime))
    implicit val pfLocalTimeMillis: Primitive[LocalTime] =
      ParquetField.logicalType[Int](time(false))(millisToLocalTime)(millisFromLocalTime)
    implicit val pfJodaLocalTimeMillis: Primitive[joda.LocalTime] =
      ParquetField.logicalType[Int](time(false))(millisToJodaLocalTime)(millisFromJodaLocalTime)

    // Deprecated alias for backward compatibility
    @deprecated("Use pfInstantMillis instead", "0.9.0")
    val pfTimestampMillis: Primitive[Instant] = pfInstantMillis
  }

  object micros extends TimeTypes {
    override protected val unit = TimeUnit.MICROS

    // TIMESTAMP
    implicit val pfInstantMicros: Primitive[Instant] =
      ParquetField.logicalType[Long](ts(true))(microsToInstant)(microsFromInstant)
    implicit val pfJodaInstantMicros: Primitive[joda.Instant] =
      ParquetField.logicalType[Long](ts(true))(microsToJodaInstant)(microsFromJodaInstant)
    implicit val pfLocalDateTimeMicros: Primitive[LocalDateTime] =
      ParquetField.logicalType[Long](ts(false))(microsToLocalDateTime)(microsFromLocalDateTime)
    implicit val pfJodaLocalDateTimeMicros: Primitive[joda.LocalDateTime] =
      ParquetField.logicalType[Long](ts(false))(microsToJodaLocalDateTime)(
        microsFromJodaLocalDateTime
      )

    // TIME
    implicit val pfOffsetTimeMicros: Primitive[OffsetTime] =
      ParquetField.logicalType[Long](time(true))(micros =>
        microsToLocalTime(micros).atOffset(ZoneOffset.UTC)
      )(ot => microsFromLocalTime(ot.toLocalTime))
    implicit val pfLocalTimeMicros: Primitive[LocalTime] =
      ParquetField.logicalType[Long](time(false))(microsToLocalTime)(microsFromLocalTime)
    implicit val pfJodaLocalTimeMicros: Primitive[joda.LocalTime] =
      ParquetField.logicalType[Long](time(false))(microsToJodaLocalTime)(microsFromJodaLocalTime)

    // Deprecated alias for backward compatibility
    @deprecated("Use pfInstantMicros instead", "0.9.0")
    val pfTimestampMicros: Primitive[Instant] = pfInstantMicros
  }

  object nanos extends TimeTypes {
    override protected val unit = TimeUnit.NANOS

    // TIMESTAMP
    implicit val pfInstantNanos: Primitive[Instant] =
      ParquetField.logicalType[Long](ts(true))(nanosToInstant)(nanosFromInstant)
    implicit val pfJodaInstantNanos: Primitive[joda.Instant] =
      ParquetField.logicalType[Long](ts(true))(nanosToJodaInstant)(nanosFromJodaInstant)
    implicit val pfLocalDateTimeNanos: Primitive[LocalDateTime] =
      ParquetField.logicalType[Long](ts(false))(nanosToLocalDateTime)(nanosFromLocalDateTime)
    implicit val pfJodaLocalDateTimeNanos: Primitive[joda.LocalDateTime] =
      ParquetField.logicalType[Long](ts(false))(nanosToJodaLocalDateTime)(
        nanosFromJodaLocalDateTime
      )

    // TIME
    implicit val pfOffsetTimeNanos: Primitive[OffsetTime] =
      ParquetField.logicalType[Long](time(true))(ns =>
        nanosToLocalTime(ns).atOffset(ZoneOffset.UTC)
      )(ot => nanosFromLocalTime(ot.toLocalTime))
    implicit val pfLocalTimeNanos: Primitive[LocalTime] =
      ParquetField.logicalType[Long](time(false))(nanosToLocalTime)(nanosFromLocalTime)
    implicit val pfJodaLocalTimeNanos: Primitive[joda.LocalTime] =
      ParquetField.logicalType[Long](time(false))(nanosToJodaLocalTime)(nanosFromJodaLocalTime)

    // Deprecated alias for backward compatibility
    @deprecated("Use pfInstantNanos instead", "0.9.0")
    val pfTimestampNanos: Primitive[Instant] = pfInstantNanos
  }
}

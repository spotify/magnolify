/*
 * Copyright 2024 Spotify AB
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

package magnolify.beam.logical

import magnolify.beam.RowField
import magnolify.shared.Time._
import org.apache.beam.sdk.schemas.Schema.FieldType
import org.apache.beam.sdk.schemas.logicaltypes
import org.joda.time as joda

import java.time as jt

// Mappings that Beam represents identically regardless of the instant encoding, shared
// between the default precision objects and their `legacy` counterparts.

private[logical] trait MillisNonInstant {
  implicit val rfLocalTimeMillis: RowField[jt.LocalTime] =
    RowField.from[Int](millisToLocalTime)(millisFromLocalTime)
  implicit val rfJodaLocalTimeMillis: RowField[joda.LocalTime] =
    RowField.from[Int](millisToJodaLocalTime)(millisFromJodaLocalTime)

  implicit val rfLocalDateTimeMillis: RowField[jt.LocalDateTime] =
    RowField.id[jt.LocalDateTime](_ => FieldType.logicalType(new logicaltypes.DateTime()))
  implicit val rfJodaLocalDateTimeMillis: RowField[joda.LocalDateTime] =
    RowField.from[jt.LocalDateTime](ldt => millisToJodaLocalDateTime(millisFromLocalDateTime(ldt)))(
      ldt => millisToLocalDateTime(millisFromJodaLocalDateTime(ldt))
    )

  implicit val rfDurationMillis: RowField[jt.Duration] =
    RowField.from[Long](millisToDuration)(millisFromDuration)
  implicit val rfJodaDurationMillis: RowField[joda.Duration] =
    RowField.from[Long](millisToJodaDuration)(millisFromJodaDuration)
}

private[logical] trait MicrosNonInstant {
  implicit val rfLocalTimeMicros: RowField[jt.LocalTime] =
    RowField.from[Long](microsToLocalTime)(microsFromLocalTime)
  // joda.LocalTime only has millisecond resolution, so excess precision is discarded
  implicit val rfJodaLocalTimeMicros: RowField[joda.LocalTime] =
    RowField.from[Long](microsToJodaLocalTime)(microsFromJodaLocalTime)

  implicit val rfLocalDateTimeMicros: RowField[jt.LocalDateTime] =
    RowField.from[Long](microsToLocalDateTime)(microsFromLocalDateTime)
  // joda.LocalDateTime has millisecond precision, excess precision discarded
  implicit val rfJodaLocalDateTimeMicros: RowField[joda.LocalDateTime] =
    RowField.from[Long](microsToJodaLocalDateTime)(microsFromJodaLocalDateTime)

  implicit val rfDurationMicros: RowField[jt.Duration] =
    RowField.from[Long](microsToDuration)(microsFromDuration)
  // joda.Duration has millisecond precision, excess precision discarded
  implicit val rfJodaDurationMicros: RowField[joda.Duration] =
    RowField.from[Long](microsToJodaDuration)(microsFromJodaDuration)
}

private[logical] trait NanosNonInstant {
  implicit val rfLocalTimeNanos: RowField[jt.LocalTime] =
    RowField.id[jt.LocalTime](_ => FieldType.logicalType(new logicaltypes.Time()))
  // joda.LocalTime only has millisecond resolution, so excess precision is discarded
  implicit val rfJodaLocalTimeNanos: RowField[joda.LocalTime] =
    RowField.from[jt.LocalTime](lt => nanosToJodaLocalTime(nanosFromLocalTime(lt)))(lt =>
      nanosToLocalTime(nanosFromJodaLocalTime(lt))
    )

  implicit val rfLocalDateTimeNanos: RowField[jt.LocalDateTime] =
    RowField.from[Long](nanosToLocalDateTime)(nanosFromLocalDateTime)
  // joda.LocalDateTime has millisecond precision, excess precision discarded
  // NOTE: misnamed `Micros` since 0.9; kept for source/binary compatibility
  implicit val rfJodaLocalDateTimeMicros: RowField[joda.LocalDateTime] =
    RowField.from[jt.LocalDateTime](ldt => nanosToJodaLocalDateTime(nanosFromLocalDateTime(ldt)))(
      ldt => nanosToLocalDateTime(nanosFromJodaLocalDateTime(ldt))
    )

  implicit val rfDurationNanos: RowField[jt.Duration] =
    RowField.id[jt.Duration](_ => FieldType.logicalType(new logicaltypes.NanosDuration()))
  // joda.Duration has millisecond precision, excess precision discarded
  implicit val rfJodaDurationNanos: RowField[joda.Duration] =
    RowField.from[jt.Duration](d => nanosToJodaDuration(nanosFromDuration(d)))(d =>
      nanosToDuration(nanosFromJodaDuration(d))
    )
}

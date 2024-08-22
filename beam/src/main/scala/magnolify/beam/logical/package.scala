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

package magnolify.beam

import org.joda.time as joda
import java.time as jt

package object logical {
  import magnolify.shared.Time._

  object millis {
    // joda
    // DATETIME	A timestamp represented as milliseconds since the epoch
    // joda.DateTime only has millisecond resolution
    implicit val bsfJodaDateTimeMillis: BeamSchemaField[joda.DateTime] =
      BeamSchemaField.from[Long](millisToJodaDateTime)(millisFromJodaDateTime)
    // DATETIME	A timestamp represented as milliseconds since the epoch
    implicit val bsfJodaInstantMillis: BeamSchemaField[joda.Instant] =
      BeamSchemaField.from[Long](millisToJodaInstant)(millisFromJodaInstant)
    implicit val bsfJodaDurationMillis: BeamSchemaField[joda.Duration] =
      BeamSchemaField.from[Long](millisToJodaDuration)(millisFromJodaDuration)
    implicit val bsfJodaLocalTimeMillis: BeamSchemaField[joda.LocalTime] =
      BeamSchemaField.from[Int](millisToJodaLocalTime)(millisFromJodaLocalTime)
    // java
    implicit val bsfInstantMillis: BeamSchemaField[jt.Instant] =
      BeamSchemaField.from[Long](millisToInstant)(millisFromInstant)
    implicit val bsLocalTimeMillis: BeamSchemaField[jt.LocalTime] =
      BeamSchemaField.from[Int](millisToLocalTime)(millisFromLocalTime)
    implicit val bsfLocalDateTimeMillis: BeamSchemaField[jt.LocalDateTime] =
      BeamSchemaField.from[Long](millisToLocalDateTime)(millisFromLocalDateTime)
    implicit val bsfDurationMillis: BeamSchemaField[jt.Duration] =
      BeamSchemaField.from[Long](millisToDuration)(millisFromDuration)
  }

  object micros {
    // joda.DateTime only has millisecond resolution, so excess precision is discarded
    implicit val bsfJodaDateTimeMicros: BeamSchemaField[joda.DateTime] =
      BeamSchemaField.from[Long](microsToJodaDateTime)(microsFromJodaDateTime)
    // joda.Instant has millisecond precision, excess precision discarded
    implicit val bsfJodaInstantMicros: BeamSchemaField[joda.Instant] =
      BeamSchemaField.from[Long](microsToJodaInstant)(microsFromJodaInstant)
    // joda.Duration has millisecond precision, excess precision discarded
    implicit val bsfJodaDurationMicros: BeamSchemaField[joda.Duration] =
      BeamSchemaField.from[Long](microsToJodaDuration)(microsFromJodaDuration)
    implicit val bsfJodaLocalTimeMicros: BeamSchemaField[joda.LocalTime] =
      BeamSchemaField.from[Long](microsToJodaLocalTime)(microsFromJodaLocalTime)
    // java
    implicit val bsfInstantMicros: BeamSchemaField[jt.Instant] =
      BeamSchemaField.from[Long](microsToInstant)(microsFromInstant)
    implicit val bsLocalTimeMicros: BeamSchemaField[jt.LocalTime] =
      BeamSchemaField.from[Long](microsToLocalTime)(microsFromLocalTime)
    implicit val bsfLocalDateTimeMicros: BeamSchemaField[jt.LocalDateTime] =
      BeamSchemaField.from[Long](microsToLocalDateTime)(microsFromLocalDateTime)
    implicit val bsfDurationMicros: BeamSchemaField[jt.Duration] =
      BeamSchemaField.from[Long](microsToDuration)(microsFromDuration)
  }

  object nanos {
    // joda.DateTime only has millisecond resolution
    implicit val bsfJodaDateTimeNanos: BeamSchemaField[joda.DateTime] =
      BeamSchemaField.from[Long](nanosToJodaDateTime)(nanosFromJodaDateTime)
    implicit val bsfJodaInstantNanos: BeamSchemaField[joda.Instant] =
      BeamSchemaField.from[Long](nanosToJodaInstant)(nanosFromJodaInstant)
    implicit val bsfJodaDurationNanos: BeamSchemaField[joda.Duration] =
      BeamSchemaField.from[Long](nanosToJodaDuration)(nanosFromJodaDuration)
    implicit val bsfJodaLocalTimeNanos: BeamSchemaField[joda.LocalTime] =
      BeamSchemaField.from[Long](nanosToJodaLocalTime)(nanosFromJodaLocalTime)
    // java
    implicit val bsfInstantNanos: BeamSchemaField[jt.Instant] =
      BeamSchemaField.from[Long](nanosToInstant)(nanosFromInstant)
    implicit val bsLocalTimeNanos: BeamSchemaField[jt.LocalTime] =
      BeamSchemaField.from[Long](nanosToLocalTime)(nanosFromLocalTime)
    implicit val bsfLocalDateTimeNanos: BeamSchemaField[jt.LocalDateTime] =
      BeamSchemaField.from[Long](nanosToLocalDateTime)(nanosFromLocalDateTime)
    implicit val bsfDurationNanos: BeamSchemaField[jt.Duration] =
      BeamSchemaField.from[Long](nanosToDuration)(nanosFromDuration)
  }
}

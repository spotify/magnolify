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

import org.apache.beam.sdk.schemas.logicaltypes
import org.apache.beam.sdk.schemas.Schema.FieldType
import org.joda.time as joda

import java.time as jt

package object logical {
  import magnolify.shared.Time._

  object millis {
    implicit val bsfInstantMillis: BeamSchemaField[jt.Instant] =
      BeamSchemaField.id[jt.Instant](_ => FieldType.DATETIME)
    implicit val bsfJodaInstantMillis: BeamSchemaField[joda.Instant] =
      BeamSchemaField.from[jt.Instant](i => millisToJodaInstant(millisFromInstant(i)))(i =>
        millisToInstant(millisFromJodaInstant(i))
      )
    // joda.DateTime only has millisecond resolution
    implicit val bsfJodaDateTimeMillis: BeamSchemaField[joda.DateTime] =
      BeamSchemaField.from[jt.Instant](i => millisToJodaDateTime(millisFromInstant(i)))(dt =>
        millisToInstant(millisFromJodaDateTime(dt))
      )

    implicit val bsLocalTimeMillis: BeamSchemaField[jt.LocalTime] =
      BeamSchemaField.from[Int](millisToLocalTime)(millisFromLocalTime)
    implicit val bsfJodaLocalTimeMillis: BeamSchemaField[joda.LocalTime] =
      BeamSchemaField.from[Int](millisToJodaLocalTime)(millisFromJodaLocalTime)

    implicit val bsfLocalDateTimeMillis: BeamSchemaField[jt.LocalDateTime] =
      BeamSchemaField.id[jt.LocalDateTime](_ => FieldType.logicalType(new logicaltypes.DateTime()))
    implicit val bsfJodaLocalDateTimeMillis: BeamSchemaField[joda.LocalDateTime] =
      BeamSchemaField.from[jt.LocalDateTime](ldt =>
        millisToJodaLocalDateTime(millisFromLocalDateTime(ldt))
      )(ldt => millisToLocalDateTime(millisFromJodaLocalDateTime(ldt)))

    implicit val bsfDurationMillis: BeamSchemaField[jt.Duration] =
      BeamSchemaField.from[Long](millisToDuration)(millisFromDuration)
    implicit val bsfJodaDurationMillis: BeamSchemaField[joda.Duration] =
      BeamSchemaField.from[Long](millisToJodaDuration)(millisFromJodaDuration)
  }

  object micros {
    // NOTE: logicaltypes.MicrosInstant() cannot be used as it throws assertion
    // errors when greater-than-microsecond precision data is used
    implicit val bsfInstantMicros: BeamSchemaField[jt.Instant] =
      BeamSchemaField.from[Long](microsToInstant)(microsFromInstant)
    // joda.Instant has millisecond precision, excess precision discarded
    implicit val bsfJodaInstantMicros: BeamSchemaField[joda.Instant] =
      BeamSchemaField.from[Long](microsToJodaInstant)(microsFromJodaInstant)
    // joda.DateTime only has millisecond resolution, so excess precision is discarded
    implicit val bsfJodaDateTimeMicros: BeamSchemaField[joda.DateTime] =
      BeamSchemaField.from[Long](microsToJodaDateTime)(microsFromJodaDateTime)

    implicit val bsfLocalTimeMicros: BeamSchemaField[jt.LocalTime] =
      BeamSchemaField.from[Long](microsToLocalTime)(microsFromLocalTime)
    implicit val bsfJodaLocalTimeMicros: BeamSchemaField[joda.LocalTime] =
      BeamSchemaField.from[Long](microsToJodaLocalTime)(microsFromJodaLocalTime)

    implicit val bsfLocalDateTimeMicros: BeamSchemaField[jt.LocalDateTime] =
      BeamSchemaField.from[Long](microsToLocalDateTime)(microsFromLocalDateTime)
    // joda.LocalDateTime has millisecond precision, excess precision discarded
    implicit val bsfJodaLocalDateTimeMicros: BeamSchemaField[joda.LocalDateTime] =
      BeamSchemaField.from[Long](microsToJodaLocalDateTime)(microsFromJodaLocalDateTime)

    implicit val bsfDurationMicros: BeamSchemaField[jt.Duration] =
      BeamSchemaField.from[Long](microsToDuration)(microsFromDuration)
    // joda.Duration has millisecond precision, excess precision discarded
    implicit val bsfJodaDurationMicros: BeamSchemaField[joda.Duration] =
      BeamSchemaField.from[Long](microsToJodaDuration)(microsFromJodaDuration)
  }

  object nanos {
    implicit val bsfInstantNanos: BeamSchemaField[jt.Instant] =
      BeamSchemaField.id[jt.Instant](_ => FieldType.logicalType(new logicaltypes.NanosInstant()))
    implicit val bsfJodaInstantNanos: BeamSchemaField[joda.Instant] =
      BeamSchemaField.from[jt.Instant](i => nanosToJodaInstant(nanosFromInstant(i)))(i =>
        nanosToInstant(nanosFromJodaInstant(i))
      )
    // joda.DateTime only has millisecond resolution
    implicit val bsfJodaDateTimeNanos: BeamSchemaField[joda.DateTime] =
      BeamSchemaField.from[jt.Instant](i => nanosToJodaDateTime(nanosFromInstant(i)))(i =>
        nanosToInstant(nanosFromJodaDateTime(i))
      )

    implicit val bsLocalTimeNanos: BeamSchemaField[jt.LocalTime] =
      BeamSchemaField.id[jt.LocalTime](_ => FieldType.logicalType(new logicaltypes.Time()))
    implicit val bsfJodaLocalTimeNanos: BeamSchemaField[joda.LocalTime] =
      BeamSchemaField.from[jt.LocalTime](lt => nanosToJodaLocalTime(nanosFromLocalTime(lt)))(lt =>
        nanosToLocalTime(nanosFromJodaLocalTime(lt))
      )

    implicit val bsfLocalDateTimeNanos: BeamSchemaField[jt.LocalDateTime] =
      BeamSchemaField.from[Long](nanosToLocalDateTime)(nanosFromLocalDateTime)
    // joda.LocalDateTime has millisecond precision, excess precision discarded
    implicit val bsfJodaLocalDateTimeMicros: BeamSchemaField[joda.LocalDateTime] =
      BeamSchemaField.from[jt.LocalDateTime](ldt =>
        nanosToJodaLocalDateTime(nanosFromLocalDateTime(ldt))
      )(ldt => nanosToLocalDateTime(nanosFromJodaLocalDateTime(ldt)))

    implicit val bsfDurationNanos: BeamSchemaField[jt.Duration] =
      BeamSchemaField.id[jt.Duration](_ => FieldType.logicalType(new logicaltypes.NanosDuration()))
    // joda.Duration has millisecond precision, excess precision discarded
    implicit val bsfJodaDurationNanos: BeamSchemaField[joda.Duration] =
      BeamSchemaField.from[jt.Duration](d => nanosToJodaDuration(nanosFromDuration(d)))(d =>
        nanosToDuration(nanosFromJodaDuration(d))
      )
  }
}

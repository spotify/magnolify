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
import org.apache.beam.sdk.schemas.logicaltypes.SqlTypes
import org.joda.time as joda
import org.joda.time.chrono.ISOChronology

import java.time as jt
import java.time.temporal.ChronoField

package object logical {
  import magnolify.shared.Time._

  object date {
    implicit val rfLocalDate: RowField[jt.LocalDate] =
      RowField.id[jt.LocalDate](_ => FieldType.logicalType(new logicaltypes.Date))
    private lazy val EpochJodaDate = new joda.LocalDate(1970, 1, 1)
    implicit val rfJodaLocalDate: RowField[joda.LocalDate] =
      RowField.from[jt.LocalDate](jtld =>
        EpochJodaDate.plusDays(jtld.getLong(ChronoField.EPOCH_DAY).toInt)
      )(d => jt.LocalDate.ofEpochDay(joda.Days.daysBetween(EpochJodaDate, d).getDays.toLong))
  }

  object millis {
    implicit lazy val rfInstantMillis: RowField[jt.Instant] =
      RowField.from[joda.Instant](i => millisToInstant(millisFromJodaInstant(i)))(i =>
        millisToJodaInstant(millisFromInstant(i))
      )
    implicit val rfJodaInstantMillis: RowField[joda.Instant] =
      RowField.id[joda.Instant](_ => FieldType.DATETIME)
    implicit val rfJodaDateTimeMillis: RowField[joda.DateTime] =
      RowField.from[joda.Instant](_.toDateTime(ISOChronology.getInstanceUTC))(_.toInstant)

    implicit val rfLocalTimeMillis: RowField[jt.LocalTime] =
      RowField.from[Int](millisToLocalTime)(millisFromLocalTime)
    implicit val rfJodaLocalTimeMillis: RowField[joda.LocalTime] =
      RowField.from[Int](millisToJodaLocalTime)(millisFromJodaLocalTime)

    implicit val rfLocalDateTimeMillis: RowField[jt.LocalDateTime] =
      RowField.id[jt.LocalDateTime](_ => FieldType.logicalType(new logicaltypes.DateTime()))
    implicit val rfJodaLocalDateTimeMillis: RowField[joda.LocalDateTime] =
      RowField.from[jt.LocalDateTime](ldt =>
        millisToJodaLocalDateTime(millisFromLocalDateTime(ldt))
      )(ldt => millisToLocalDateTime(millisFromJodaLocalDateTime(ldt)))

    implicit val rfDurationMillis: RowField[jt.Duration] =
      RowField.from[Long](millisToDuration)(millisFromDuration)
    implicit val rfJodaDurationMillis: RowField[joda.Duration] =
      RowField.from[Long](millisToJodaDuration)(millisFromJodaDuration)
  }

  object micros {
    // NOTE: logicaltypes.MicrosInstant() cannot be used as it throws assertion
    // errors when greater-than-microsecond precision data is used
    implicit val rfInstantMicros: RowField[jt.Instant] =
      RowField.from[Long](microsToInstant)(microsFromInstant)
    // joda.Instant has millisecond precision, excess precision discarded
    implicit val rfJodaInstantMicros: RowField[joda.Instant] =
      RowField.from[Long](microsToJodaInstant)(microsFromJodaInstant)
    // joda.DateTime only has millisecond resolution, so excess precision is discarded
    implicit val rfJodaDateTimeMicros: RowField[joda.DateTime] =
      RowField.from[Long](microsToJodaDateTime)(microsFromJodaDateTime)

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

  object nanos {
    implicit val rfInstantNanos: RowField[jt.Instant] =
      RowField.id[jt.Instant](_ => FieldType.logicalType(new logicaltypes.NanosInstant()))
    // joda.Instant has millisecond precision, excess precision discarded
    implicit val rfJodaInstantNanos: RowField[joda.Instant] =
      RowField.from[jt.Instant](i => nanosToJodaInstant(nanosFromInstant(i)))(i =>
        nanosToInstant(nanosFromJodaInstant(i))
      )
    // joda.DateTime only has millisecond resolution
    implicit val rfJodaDateTimeNanos: RowField[joda.DateTime] =
      RowField.from[jt.Instant](i => nanosToJodaDateTime(nanosFromInstant(i)))(i =>
        nanosToInstant(nanosFromJodaDateTime(i))
      )

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

  object sql {
    implicit val rfSqlLocalTime: RowField[jt.LocalTime] =
      RowField.id(_ => FieldType.logicalType(SqlTypes.TIME))
    implicit val rfSqlInstant: RowField[jt.Instant] =
      RowField.id(_ => FieldType.logicalType(SqlTypes.TIMESTAMP))
    implicit val rfSqlLocalDateTime: RowField[jt.LocalDateTime] =
      RowField.id(_ => FieldType.logicalType(SqlTypes.DATETIME))
    implicit val rfSqlLocalDate: RowField[jt.LocalDate] =
      RowField.id(_ => FieldType.logicalType(SqlTypes.DATE))
  }
}

/*
 * Copyright 2026 Spotify AB
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
import org.joda.time.chrono.ISOChronology

import java.time as jt

// The pre-0.10 `Instant` encodings, shared by `compat.*` and by the deprecated bare
// `millis`/`micros`/`nanos` objects so that the two cannot drift apart.
//
// Note these three do not share a representation -- millis is the joda-backed `DATETIME`
// primitive, micros is a raw `INT64` and nanos is the SDK-local `NanosInstant` logical type.
// What they have in common is only that this is what 0.9 produced, which is why the grouping
// is named for its history rather than for an encoding.

private[logical] trait MillisCompat extends MillisNonInstant {
  // Layered on the joda mapping below, so the typeclass's internal representation is joda.
  // `lazy` because it resolves `rfJodaInstantMillis`, which is declared after it.
  implicit lazy val rfInstantMillis: RowField[jt.Instant] =
    RowField.from[joda.Instant](i => millisToInstant(millisFromJodaInstant(i)))(i =>
      millisToJodaInstant(millisFromInstant(i))
    )
  implicit val rfJodaInstantMillis: RowField[joda.Instant] =
    RowField.id[joda.Instant](_ => FieldType.DATETIME)
  implicit val rfJodaDateTimeMillis: RowField[joda.DateTime] =
    RowField.from[joda.Instant](_.toDateTime(ISOChronology.getInstanceUTC))(_.toInstant)
}

private[logical] trait MicrosCompat extends MicrosNonInstant {
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
}

private[logical] trait NanosCompat extends NanosNonInstant {
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
}

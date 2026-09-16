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
import org.apache.beam.sdk.schemas.logicaltypes.{SqlTypes, Timestamp}
import org.joda.time as joda
import org.joda.time.chrono.ISOChronology

import java.time as jt
import java.time.temporal.ChronoUnit

package object logical {
  import magnolify.shared.Time._

  object date {
    implicit val rfLocalDate: RowField[jt.LocalDate] =
      RowField.id[jt.LocalDate](_ => FieldType.logicalType(new logicaltypes.Date))
    implicit val rfJodaLocalDate: RowField[joda.LocalDate] =
      RowField.from[jt.LocalDate](localDateToJodaLocalDate)(jodaLocalDateToLocalDate)
  }

  // Timestamp#toBaseType throws when an instant carries finer precision than the type
  // declares, so writes truncate instead. Excess precision is discarded, matching the
  // behavior of the non-instant mappings at each precision.
  private def tsInstant(ts: Timestamp, unit: ChronoUnit): RowField[jt.Instant] = {
    implicit val base: RowField[jt.Instant] =
      RowField.id[jt.Instant](_ => FieldType.logicalType(ts))
    RowField.from[jt.Instant](identity)(_.truncatedTo(unit))
  }

  /**
   * Millisecond-precision temporal mappings.
   *
   * `Instant` maps to Beam's portable `Timestamp.MILLIS` logical type. Instants carrying finer
   * precision are truncated on write, because `Timestamp` rejects them rather than rounding.
   *
   * Not writable to Iceberg: its schema conversion accepts only `Timestamp.MICROS` (precision 6)
   * and throws `UnsupportedOperationException` otherwise. For Iceberg use [[micros]], or
   * [[legacy.millis]] when the pipeline pins `--updateCompatibilityVersion` below 2.76.0.
   *
   * Prior to 0.10 this mapped to `FieldType.DATETIME`, backed by `org.joda.time.Instant`; see
   * [[legacy.millis]].
   */
  object millis extends MillisNonInstant {
    implicit val rfInstantMillis: RowField[jt.Instant] =
      tsInstant(Timestamp.MILLIS, ChronoUnit.MILLIS)
    implicit val rfJodaInstantMillis: RowField[joda.Instant] =
      RowField.from[jt.Instant](i => millisToJodaInstant(millisFromInstant(i)))(i =>
        millisToInstant(millisFromJodaInstant(i))
      )(rfInstantMillis)
    implicit val rfJodaDateTimeMillis: RowField[joda.DateTime] =
      RowField.from[joda.Instant](_.toDateTime(ISOChronology.getInstanceUTC))(_.toInstant)(
        rfJodaInstantMillis
      )
  }

  /**
   * Microsecond-precision temporal mappings.
   *
   * `Instant` maps to Beam's portable `Timestamp.MICROS` logical type. Instants carrying finer
   * precision are truncated on write, because `Timestamp` rejects them rather than rounding.
   *
   * This is the encoding IcebergIO both produces and accepts for `timestamptz` as of Beam 2.76.0,
   * making it the right choice for Iceberg — unless the pipeline pins
   * `--updateCompatibilityVersion` below 2.76.0, in which case see [[legacy.millis]].
   *
   * Prior to 0.10 this mapped to a raw `INT64` of microseconds since epoch; see [[legacy.micros]].
   */
  object micros extends MicrosNonInstant {
    implicit val rfInstantMicros: RowField[jt.Instant] =
      tsInstant(Timestamp.MICROS, ChronoUnit.MICROS)
    // joda.Instant has millisecond precision, excess precision discarded
    implicit val rfJodaInstantMicros: RowField[joda.Instant] =
      RowField.from[jt.Instant](i => microsToJodaInstant(microsFromInstant(i)))(i =>
        microsToInstant(microsFromJodaInstant(i))
      )(rfInstantMicros)
    // joda.DateTime only has millisecond resolution, so excess precision is discarded
    implicit val rfJodaDateTimeMicros: RowField[joda.DateTime] =
      RowField.from[jt.Instant](i => microsToJodaDateTime(microsFromInstant(i)))(dt =>
        microsToInstant(microsFromJodaDateTime(dt))
      )(rfInstantMicros)
  }

  /**
   * Nanosecond-precision temporal mappings.
   *
   * `Instant` maps to Beam's portable `Timestamp.NANOS` logical type, which holds the full
   * precision of `java.time.Instant`, so nothing is truncated.
   *
   * Not writable to Iceberg, which accepts only `Timestamp.MICROS`; use [[micros]] instead. The
   * pre-0.10 `NanosInstant` encoding was not Iceberg-writable either, so this is not a regression.
   *
   * Prior to 0.10 this mapped to the SDK-local `NanosInstant` logical type; see [[legacy.nanos]].
   */
  object nanos extends NanosNonInstant {
    implicit val rfInstantNanos: RowField[jt.Instant] =
      tsInstant(Timestamp.NANOS, ChronoUnit.NANOS)
    // joda.Instant has millisecond precision, excess precision discarded
    implicit val rfJodaInstantNanos: RowField[joda.Instant] =
      RowField.from[jt.Instant](i => nanosToJodaInstant(nanosFromInstant(i)))(i =>
        nanosToInstant(nanosFromJodaInstant(i))
      )(rfInstantNanos)
    // joda.DateTime only has millisecond resolution
    implicit val rfJodaDateTimeNanos: RowField[joda.DateTime] =
      RowField.from[jt.Instant](i => nanosToJodaDateTime(nanosFromInstant(i)))(i =>
        nanosToInstant(nanosFromJodaDateTime(i))
      )(rfInstantNanos)
  }

  /**
   * Instant encodings used before 0.10: joda-backed `FieldType.DATETIME` at millis, a raw `INT64`
   * of microseconds at micros, and the SDK-local `NanosInstant` at nanos.
   *
   * These are the magnolify-side counterpart to Beam's `--updateCompatibilityVersion` flag. A
   * pipeline pinned below 2.76.0 gets `FieldType.DATETIME` back from IcebergIO, which only
   * [[legacy.millis]] can read; the default objects expect the portable `Timestamp` type and will
   * not match. Pair the flag with `legacy`, or omit both — mixing them is the one broken
   * combination.
   *
   * Also needed for connectors that emit `FieldType.DATETIME` irrespective of the flag. As of Beam
   * 2.76.0, within `sdks/java/io` that is amazon-web-services2, clickhouse, csv, delta,
   * google-cloud-platform, hcatalog, iceberg, jdbc and singlestore; `DATETIME` is additionally
   * produced by core and by the arrow, avro, protobuf and sql-datacatalog extensions.
   *
   * Non-instant mappings are identical to the defaults.
   */
  object legacy {
    object millis extends MillisNonInstant {
      implicit lazy val rfInstantMillis: RowField[jt.Instant] =
        RowField.from[joda.Instant](i => millisToInstant(millisFromJodaInstant(i)))(i =>
          millisToJodaInstant(millisFromInstant(i))
        )
      implicit val rfJodaInstantMillis: RowField[joda.Instant] =
        RowField.id[joda.Instant](_ => FieldType.DATETIME)
      implicit val rfJodaDateTimeMillis: RowField[joda.DateTime] =
        RowField.from[joda.Instant](_.toDateTime(ISOChronology.getInstanceUTC))(_.toInstant)
    }

    object micros extends MicrosNonInstant {
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

    object nanos extends NanosNonInstant {
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
  }

  @deprecated(
    "SqlTypes.DATE/TIME/DATETIME duplicate `date` and the precision objects, and " +
      "SqlTypes.TIMESTAMP is MicrosInstant, which throws on sub-microsecond instants. " +
      "Use `date` plus one of millis/micros/nanos instead.",
    "0.10.0"
  )
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

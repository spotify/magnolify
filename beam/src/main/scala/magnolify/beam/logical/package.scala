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
   * Temporal mappings that encode `Instant` with Beam's portable `Timestamp` logical type
   * (`beam:logical_type:timestamp:v1`) at the precision of the object you import.
   *
   * This is the encoding IcebergIO both produces and accepts for `timestamptz` as of Beam 2.76.0,
   * which is the reason these mappings exist.
   *
   * Beam's IOs do not agree on a precision, so the object you import determines which of them you
   * can write to. As of Beam 2.76.0: IcebergIO requires [[timestamp.micros]]; BigQueryIO and the
   * Avro extension require [[timestamp.nanos]]; managed JDBC (postgres/mysql/sqlserver) and Kafka
   * with `JSON` format accept no `Timestamp` precision at all and need [[compat]]. Beam SQL accepts
   * any precision but round-trips values through milliseconds, discarding anything finer.
   *
   * `Timestamp` rejects instants carrying finer precision than it declares rather than rounding
   * them, so [[timestamp.millis]] and [[timestamp.micros]] truncate on write.
   *
   * Non-instant mappings are identical to [[compat]].
   */
  object timestamp {

    /**
     * `Instant` maps to `Timestamp.MILLIS`. Instants carrying finer precision are truncated on
     * write.
     *
     * No Beam IO accepts precision 3 on write: IcebergIO requires 6, BigQueryIO and the Avro
     * extension require 9. Its use is reading data that is already precision 3 — notably a BigQuery
     * `TIMESTAMP(12)` column under `--picosecondTimestampMapping=MILLIS`. To *write*
     * millisecond-precision instants, use [[compat.millis]], whose `DATETIME` encoding every
     * schema-aware Beam IO accepts.
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
     * `Instant` maps to `Timestamp.MICROS`. Instants carrying finer precision are truncated on
     * write.
     *
     * This is what IcebergIO produces and accepts for `timestamptz` as of Beam 2.76.0, making it
     * the right choice for Iceberg — unless the pipeline pins `--updateCompatibilityVersion` below
     * 2.76.0, in which case use [[compat.millis]].
     *
     * Not writable via Beam's BigQueryIO or Avro extension, which require precision 9; use
     * [[timestamp.nanos]] there. No single precision satisfies both. (This concerns Beam's own
     * BigQueryIO on `Row`; magnolify's `bigquery` module converts to `TableRow` and is unaffected.)
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
     * `Instant` maps to `Timestamp.NANOS`, which holds the full precision of `java.time.Instant`,
     * so nothing is truncated. This is the precision Beam's BigQueryIO and Avro extension require.
     *
     * Not writable to Iceberg, which accepts only `Timestamp.MICROS`; use [[timestamp.micros]]
     * instead. The pre-0.10 `NanosInstant` encoding was not Iceberg-writable either, so this is no
     * regression.
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
  }

  /**
   * The `Instant` encodings magnolify produced before 0.10: the joda-backed `FieldType.DATETIME`
   * primitive at [[compat.millis]], a raw `INT64` of microseconds at [[compat.micros]], and the
   * SDK-local `NanosInstant` logical type at [[compat.nanos]].
   *
   * These three share no representation — only their history. The grouping is named for the
   * compatibility it provides rather than for an encoding, because there is no encoding common to
   * all three.
   *
   * [[compat.millis]] is the magnolify-side counterpart to Beam's `--updateCompatibilityVersion`
   * flag. A pipeline pinned below 2.76.0 gets `FieldType.DATETIME` back from IcebergIO, which only
   * [[compat.millis]] can read; [[timestamp]] expects the portable `Timestamp` type and will not
   * match. Pair the flag with `compat.millis`, or omit both — mixing them is the one broken
   * combination.
   *
   * `DATETIME` is also the only `Instant` encoding that every schema-aware Beam IO accepts, so
   * [[compat.millis]] is not merely a migration aid. It is required for connectors that hardcode
   * `DATETIME`: as of Beam 2.76.0, within `sdks/java/io` that is amazon-web-services2, clickhouse,
   * delta, google-cloud-platform, hcatalog, iceberg, jdbc and singlestore, plus core and the arrow,
   * avro, sql and sql-datacatalog extensions. Beyond those, schema inference maps any joda
   * `Instant` field to `DATETIME` (`FieldTypeDescriptors`), so a connector whose element type has
   * joda fields produces it without naming the type — KafkaIO's `KafkaSourceDescriptor` is one. And
   * it is the only option for the managed JDBC sinks and for Kafka with `JSON` format, neither of
   * which handles any `Timestamp` precision.
   *
   * Non-instant mappings are identical to [[timestamp]].
   */
  object compat {
    object millis extends MillisCompat
    object micros extends MicrosCompat
    object nanos extends NanosCompat
  }

  @deprecated(
    "Renamed to `compat.millis` so the encoding it produces is explicit. This object is " +
      "unchanged: `Instant` still maps to the joda-backed `FieldType.DATETIME` primitive. " +
      "Use `timestamp.micros` for IcebergIO on Beam 2.76.0+, or `compat.millis` to keep this " +
      "encoding.",
    "0.10.0"
  )
  object millis extends MillisCompat

  @deprecated(
    "Renamed to `compat.micros` so the encoding it produces is explicit. This object is " +
      "unchanged: `Instant` still maps to a raw `INT64` of microseconds since epoch. " +
      "Use `timestamp.micros` for Beam's portable `Timestamp` logical type, or `compat.micros` " +
      "to keep this encoding.",
    "0.10.0"
  )
  object micros extends MicrosCompat

  @deprecated(
    "Renamed to `compat.nanos` so the encoding it produces is explicit. This object is " +
      "unchanged: `Instant` still maps to the SDK-local `NanosInstant` logical type. " +
      "Use `timestamp.nanos` for Beam's portable `Timestamp` logical type, or `compat.nanos` " +
      "to keep this encoding.",
    "0.10.0"
  )
  object nanos extends NanosCompat

  @deprecated(
    "SqlTypes.DATE/TIME/DATETIME duplicate `date` and the precision objects, and " +
      "SqlTypes.TIMESTAMP is MicrosInstant, which throws on sub-microsecond instants. " +
      "Use `date` plus one of timestamp.{millis,micros,nanos} or compat.{millis,micros,nanos} " +
      "instead.",
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

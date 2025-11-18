/*
 * Copyright 2020 Spotify AB
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

package magnolify.bigquery

import java.time._
import java.time.format._
import java.time.temporal.ChronoField

// https://github.com/googleapis/java-bigquery/blob/master/google-cloud-bigquery/src/main/java/com/google/cloud/bigquery/QueryParameterValue.java
private object TimestampConverter {
  // TIME
  // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#time_type
  // [H]H:[M]M:[S]S[.DDDDDD|.F]
  private val timeFormatter = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NEVER)
    .appendLiteral(':')
    .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NEVER)
    .appendLiteral(':')
    .appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, SignStyle.NEVER)
    .optionalStart()
    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
    .optionalStart()
    .toFormatter()

  private val toTimeFormatter = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.HOUR_OF_DAY, 2, 2, SignStyle.NEVER)
    .appendLiteral(':')
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2, 2, SignStyle.NEVER)
    .appendLiteral(':')
    .appendValue(ChronoField.SECOND_OF_MINUTE, 2, 2, SignStyle.NEVER)
    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
    .toFormatter()

  // DATE
  // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#date_type
  // YYYY-[M]M-[D]D
  private val dateFormatter = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR, 4)
    .appendLiteral('-')
    .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NEVER)
    .appendLiteral('-')
    .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NEVER)
    .toFormatter()

  private val toDateFormatter = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR, 4)
    .appendLiteral('-')
    .appendValue(ChronoField.MONTH_OF_YEAR, 2, 2, SignStyle.NEVER)
    .appendLiteral('-')
    .appendValue(ChronoField.DAY_OF_MONTH, 2, 2, SignStyle.NEVER)
    .toFormatter()

  // TIMESTAMP
  // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#time_type

  // civil_date_part YYYY-[M]M-[D]D
  private val civilDatePartFormatter = dateFormatter
  // time_part { |T|t}[H]H:[M]M:[S]S[.F]
  private val timePartFormatter =
    new DateTimeFormatterBuilder()
      .padNext(1)
      .optionalStart()
      .parseCaseInsensitive()
      .appendLiteral('T')
      .parseCaseSensitive()
      .optionalEnd()
      .append(timeFormatter)
      .toFormatter

  // time_zone_offset or utc_time_zone
  // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#time_zones
  // {+|-}H[H][:M[M]] or {Z|z}
  private val timeZoneOffsetFormatter = new DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendOffsetId()
    .parseCaseSensitive()
    .toFormatter

  // time_zone
  private val timeZoneFormatter = new DateTimeFormatterBuilder()
    .appendZoneRegionId()
    .toFormatter

  // timestamp
  // {
  //  civil_date_part[time_part [time_zone]] |
  //  civil_date_part[time_part[time_zone_offset]] |
  //  civil_date_part[time_part[utc_time_zone]]
  // }
  private val timestampFormatter =
    new DateTimeFormatterBuilder()
      .append(civilDatePartFormatter)
      .append(timePartFormatter)
      .optionalStart()
      .append(timeZoneOffsetFormatter)
      .optionalEnd()
      .optionalStart()
      .appendLiteral(' ')
      .append(timeZoneFormatter)
      .optionalEnd()
      .toFormatter
      .withZone(ZoneOffset.UTC)

  private val toTimestampFormatter: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(toDateFormatter)
      .appendLiteral('T')
      .append(toTimeFormatter)
      .appendZoneId()
      .toFormatter
      .withZone(ZoneOffset.UTC)

  // DATETIME
  // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#datetime_type
  // civil_date_part[time_part]
  private val datetimeFormatter = new DateTimeFormatterBuilder()
    .append(civilDatePartFormatter)
    .appendOptional(timePartFormatter)
    .toFormatter

  private val toDatetimeFormatter = new DateTimeFormatterBuilder()
    .append(toDateFormatter)
    .appendLiteral('T')
    .append(toTimeFormatter)
    .toFormatter

  def toInstant(v: Any): Instant = Instant.from(timestampFormatter.parse(v.toString))
  // can't use DateTimeFormatter.ISO_INSTANT; it outputs a variable number of fractional digits
  def fromInstant(i: Instant): Any = toTimestampFormatter.format(i)

  def toLocalDate(v: Any): LocalDate = LocalDate.from(dateFormatter.parse(v.toString))
  def fromLocalDate(d: LocalDate): Any = DateTimeFormatter.ISO_LOCAL_DATE.format(d)

  def toLocalTime(v: Any): LocalTime = LocalTime.from(timeFormatter.parse(v.toString))
  // can't use DateTimeFormatter.ISO_LOCAL_TIME; outputs a variable number of fractional digits
  def fromLocalTime(t: LocalTime): Any = toTimeFormatter.format(t)

  def toLocalDateTime(v: Any): LocalDateTime =
    LocalDateTime.from(datetimeFormatter.parse(v.toString))
  def fromLocalDateTime(dt: LocalDateTime): Any = toDatetimeFormatter.format(dt)
}

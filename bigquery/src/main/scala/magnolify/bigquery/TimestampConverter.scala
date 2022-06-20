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
private[bigquery] object TimestampConverter {
  // TIMESTAMP
  // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]][time zone]
  private val timestampFormatter =
    new DateTimeFormatterBuilder()
      .parseLenient()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .appendLiteral(' ')
      .appendValue(ChronoField.HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
      .optionalStart()
      .appendLiteral(':')
      .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 6, 9, true)
      .optionalStart()
      .appendOffset("+HHMM", "+00:00")
      .optionalEnd()
      .toFormatter()
      .withZone(ZoneOffset.UTC)
  private val timestampValidator =
    new DateTimeFormatterBuilder()
      .parseLenient()
      .append(timestampFormatter)
      .optionalStart()
      .appendOffsetId()
      .optionalEnd()
      .toFormatter()
      .withZone(ZoneOffset.UTC)

  // DATE
  // YYYY-[M]M-[D]D
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  // TIME
  // [H]H:[M]M:[S]S[.DDDDDD]
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS")

  // DATETIME
  // YYYY-[M]M-[D]D[ [H]H:[M]M:[S]S[.DDDDDD]]
  private val datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

  def toInstant(v: Any): Instant = Instant.from(timestampValidator.parse(v.toString))
  def fromInstant(i: Instant): Any = timestampFormatter.format(i)

  def toLocalDate(v: Any): LocalDate = LocalDate.from(dateFormatter.parse(v.toString))
  def fromLocalDate(d: LocalDate): Any = dateFormatter.format(d)

  def toLocalTime(v: Any): LocalTime = LocalTime.from(timeFormatter.parse(v.toString))
  def fromLocalTime(t: LocalTime): Any = timeFormatter.format(t)

  def toLocalDateTime(v: Any): LocalDateTime =
    LocalDateTime.from(datetimeFormatter.parse(v.toString))
  def fromLocalDateTime(dt: LocalDateTime): Any = datetimeFormatter.format(dt)
}

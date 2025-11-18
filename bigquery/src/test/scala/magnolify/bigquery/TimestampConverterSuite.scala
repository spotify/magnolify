/*
 * Copyright 2023 Spotify AB
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

import java.time.{LocalDateTime, LocalTime}
import java.time.temporal.ChronoUnit
import scala.util.control.NonFatal

class TimestampConverterSuite extends munit.ScalaCheckSuite {

  test("TIMESTAMP") {
    Seq(
      // this should be the default format sent by BQ API
      "2023-01-01 10:11:12.123456 UTC",
      // optional digits
      "2023-01-01 10:11:12.123456",
      "2023-01-01 10:11:12.1234",
      "2023-01-01 10:11:12",
      "2023-1-1 0:1:2",
      // time separator
      "2023-01-01T10:11:12.123456 UTC",
      "2023-01-01t10:11:12.123456 UTC",
      // offsets
      "2023-01-01t10:11:12.123456Z",
      "2023-01-01t10:11:12.123456z",
      "2023-01-01t10:11:12.123456+01:02",
      "2023-01-01t10:11:12.123456-01:02",
      // "2023-01-01t10:11:12.123456+1:2", not possible to get +H:M offset pattern
      // "2023-01-01t10:11:12.123456+1", skipped
      // zone
      "2023-01-01t10:11:12.123456 Europe/London",
      "2023-01-01t10:11:12.123456 UTC+01:02",
      "2023-01-01t10:11:12.123456 UTC-01:02"
      // "2023-01-01t10:11:12.123456 UTC+1:2", not possible to get +H:M offset pattern
      // "2023-01-01t10:11:12.123456 UTC+1", skipped
    ).foreach { ts =>
      try {
        TimestampConverter.toInstant(ts)
      } catch {
        case NonFatal(e) => throw new Exception(s"Failed parsing $ts", e)
      }
    }
    // formatter uses ISO
    val javaInstant = TimestampConverter.toInstant("2023-01-01 09:01:02.123456 UTC")
    val actual = TimestampConverter.fromInstant(javaInstant)
    assertEquals(actual, "2023-01-01T09:01:02.123456Z")
    // round-trips WITH TRUNCATION
    val nanosInstant = java.time.Instant.parse("2023-01-01T10:01:02.123456789Z")
    val nanosTs = TimestampConverter.fromInstant(nanosInstant)
    val nanosInstantRoundTrip = TimestampConverter.toInstant(nanosTs)
    assertEquals(nanosInstantRoundTrip, nanosInstant.truncatedTo(ChronoUnit.MICROS))
    // round-trips
    val nanosInstantShort = java.time.Instant.parse("2023-01-01T10:01:02.123Z")
    val nanosTsShort = TimestampConverter.fromInstant(nanosInstantShort)
    assertEquals(nanosTsShort.toString, "2023-01-01T10:01:02.123Z")
    val nanosInstantRoundTripShort = TimestampConverter.toInstant(nanosTsShort)
    assertEquals(nanosInstantRoundTripShort, nanosInstantShort)
  }

  test("DATE") {
    Seq(
      "2023-01-02",
      "2023-01-2",
      "2023-1-2"
    ).foreach { date =>
      try {
        TimestampConverter.toLocalDate(date)
      } catch {
        case NonFatal(e) => throw new Exception(s"Failed parsing $date", e)
      }
    }
    // formatter uses ISO
    val javaLocalDate = TimestampConverter.toLocalDate("2023-01-02")
    val actual = TimestampConverter.fromLocalDate(javaLocalDate)
    assertEquals(actual, "2023-01-02")
  }

  test("TIME") {
    Seq(
      "10:11:12.123456",
      "10:11:12.1234",
      "10:11:12",
      "0:1:2",
      "0:1:2.123456"
    ).foreach { t =>
      try {
        TimestampConverter.toLocalTime(t)
      } catch {
        case NonFatal(e) => throw new Exception(s"Failed parsing $t", e)
      }
    }
    // formatter uses ISO
    val javaLocalTime: LocalTime = TimestampConverter.toLocalTime("16:03:57.029881")
    val actual = TimestampConverter.fromLocalTime(javaLocalTime)
    assertEquals(actual, "16:03:57.029881")
    // round-trips WITH TRUNCATION
    val timeNanos = LocalTime.parse("01:02:03.123456789")
    val timeTs = TimestampConverter.fromLocalTime(timeNanos)
    assertEquals(timeTs.toString, "01:02:03.123456")
    val timeNanosRoundtrip = TimestampConverter.toLocalTime(timeTs)
    assertEquals(timeNanosRoundtrip, timeNanos.truncatedTo(ChronoUnit.MICROS))
    // round-trips
    val timeNanosShort = LocalTime.parse("01:02:03.123")
    val timeShortTs = TimestampConverter.fromLocalTime(timeNanosShort)
    assertEquals(timeShortTs.toString, "01:02:03.123")
    val timeShortRoundtrip = TimestampConverter.toLocalTime(timeShortTs)
    assertEquals(timeShortRoundtrip, timeNanosShort)
  }

  test("DATETIME") {
    Seq(
      "2023-01-01 10:11:12.123456",
      // optional digits
      "2023-01-01 10:11:12.123456",
      "2023-01-01 10:11:12.1234",
      "2023-01-01 10:11:12",
      "2023-1-1 0:1:2",
      // time separator
      "2023-01-01T10:11:12.123456",
      "2023-01-01t10:11:12.123456"
    ).foreach { datetime =>
      try {
        TimestampConverter.toLocalDateTime(datetime)
      } catch {
        case NonFatal(e) => throw new Exception(s"Failed parsing $datetime", e)
      }
    }
    // formatter uses ISO
    val javaLocalDateTime = TimestampConverter.toLocalDateTime("2023-01-01 10:11:12.123456")
    val actual = TimestampConverter.fromLocalDateTime(javaLocalDateTime)
    assertEquals(actual, "2023-01-01T10:11:12.123456")
    // round-trips WITH TRUNCATION
    val nanosDT = LocalDateTime.parse("2023-01-01T10:11:12.123456789")
    val dt = TimestampConverter.fromLocalDateTime(nanosDT)
    assertEquals(dt.toString, "2023-01-01T10:11:12.123456")
    val dtRoundtrip = TimestampConverter.toLocalDateTime(dt)
    assertEquals(dtRoundtrip, nanosDT.truncatedTo(ChronoUnit.MICROS))
    // round-trips
    val nanosShortDT = LocalDateTime.parse("2023-01-01T10:11:12.123")
    val dtShort = TimestampConverter.fromLocalDateTime(nanosShortDT)
    assertEquals(dtShort.toString, "2023-01-01T10:11:12.123")
    val dtShortRoundtrip = TimestampConverter.toLocalDateTime(dtShort)
    assertEquals(dtShortRoundtrip, nanosShortDT)
  }
}

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

package magnolify.shared

import org.joda.time as joda
import org.joda.time.chrono.ISOChronology

import java.time.temporal.ChronoField
import java.time.{Duration, Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.{MICROSECONDS, MILLISECONDS, NANOSECONDS, SECONDS}

object Time {
  @transient lazy val EpochJodaDate = new joda.LocalDate(1970, 1, 1, ISOChronology.getInstanceUTC)
  // generic ////////////////////////////////////////////////////
  @inline def unitToInstant(unit: TimeUnit, unitsFromEpoch: Long): Instant = {
    if (unit == MILLISECONDS) Instant.ofEpochMilli(unitsFromEpoch)
    else {
      val epochSeconds = unit.toSeconds(unitsFromEpoch)
      val nanoAdjustment = unit.toNanos(unitsFromEpoch - SECONDS.toMicros(epochSeconds))
      Instant.ofEpochSecond(epochSeconds, nanoAdjustment)
    }
  }

  // conversions ////////////////////////////////////////////////
  @inline def jodaInstantToInstant(jodaInstant: joda.Instant): Instant =
    Instant.ofEpochMilli(jodaInstant.getMillis)
  @inline def instantToJodaInstant(javaInstant: Instant): joda.Instant =
    new joda.Instant(javaInstant.toEpochMilli)
  @inline def jodaLocalDateToLocalDate(ld: joda.LocalDate): LocalDate =
    LocalDate.ofEpochDay(joda.Days.daysBetween(EpochJodaDate, ld).getDays.toLong)
  @inline def localDateToJodaLocalDate(ld: LocalDate): joda.LocalDate =
    EpochJodaDate.plusDays(ld.getLong(ChronoField.EPOCH_DAY).toInt)
  @inline def jodaLocalTimeToLocalTime(t: joda.LocalTime): LocalTime = {
    val nanos = MILLISECONDS.toNanos(t.getMillisOfSecond.toLong).toInt
    LocalTime.of(t.getHourOfDay, t.getMinuteOfHour, t.getSecondOfMinute, nanos)
  }
  @inline def localTimeToJodaLocalTime(t: LocalTime): joda.LocalTime = {
    val millis = NANOSECONDS.toMillis(t.getNano.toLong).toInt
    new joda.LocalTime(t.getHour, t.getMinute, t.getSecond, millis, ISOChronology.getInstanceUTC)
  }
  @inline def jodaLocalDateTimeToLocalDateTime(ldt: joda.LocalDateTime): LocalDateTime = {
    val nanos = MILLISECONDS.toNanos(ldt.getMillisOfSecond.toLong).toInt
    LocalDateTime.of(
      ldt.getYear,
      ldt.getMonthOfYear,
      ldt.getDayOfMonth,
      ldt.getHourOfDay,
      ldt.getMinuteOfHour,
      ldt.getSecondOfMinute,
      nanos
    )
  }
  @inline def localDateTimeToJodaLocalDateTime(ldt: LocalDateTime): joda.LocalDateTime = {
    val millis = NANOSECONDS.toMillis(ldt.getNano.toLong)
    new joda.LocalDateTime(
      ldt.getYear,
      ldt.getMonthValue,
      ldt.getDayOfMonth,
      ldt.getHour,
      ldt.getMinute,
      ldt.getSecond,
      millis.toInt,
      ISOChronology.getInstanceUTC
    )
  }

  // millis /////////////////////////////////////////////////////
  @inline def millisToSecondsAndNanos(millis: Long): (Long, Long) = {
    val seconds = MILLISECONDS.toSeconds(millis)
    val nanos = MILLISECONDS.toNanos(millis - SECONDS.toMillis(seconds))
    (seconds, nanos)
  }
  @inline def millisFromSecondsAndNanos(seconds: Long, nanos: Long): Long =
    SECONDS.toMillis(seconds) + NANOSECONDS.toMillis(nanos)

  @inline def millisToInstant(millisFromEpoch: Long): Instant =
    unitToInstant(MILLISECONDS, millisFromEpoch)
  @inline def millisFromInstant(instant: Instant): Long = instant.toEpochMilli
  @inline def millisToJodaInstant(millisFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(millisFromEpoch)
  @inline def millisFromJodaInstant(instant: joda.Instant): Long = instant.getMillis

  @inline def millisToLocalTime(millisFromMidnight: Int): LocalTime =
    LocalTime.ofNanoOfDay(MILLISECONDS.toNanos(millisFromMidnight.toLong))
  @inline def millisFromLocalTime(lt: LocalTime): Int =
    NANOSECONDS.toMillis(lt.toNanoOfDay).toInt
  @inline def millisToJodaLocalTime(millisFromMidnight: Int): joda.LocalTime =
    joda.LocalTime.fromMillisOfDay(millisFromMidnight.toLong, ISOChronology.getInstanceUTC)
  @inline def millisFromJodaLocalTime(lt: joda.LocalTime): Int = lt.millisOfDay().get()

  @inline def millisToJodaDateTime(millisFromEpoch: Long): joda.DateTime =
    new joda.DateTime(millisFromEpoch, joda.DateTimeZone.UTC)
  @inline def millisFromJodaDateTime(dt: joda.DateTime): Long = dt.getMillis

  @inline def millisToLocalDateTime(millisFromEpoch: Long): LocalDateTime =
    LocalDateTime.ofInstant(millisToInstant(millisFromEpoch), ZoneOffset.UTC)
  @inline def millisFromLocalDateTime(ldt: LocalDateTime): Long =
    millisFromInstant(ldt.toInstant(ZoneOffset.UTC))
  @inline def millisToJodaLocalDateTime(millisFromEpoch: Long): joda.LocalDateTime =
    new joda.LocalDateTime(millisFromEpoch, joda.DateTimeZone.UTC)
  @inline def millisFromJodaLocalDateTime(ldt: joda.LocalDateTime): Long =
    ldt.toDateTime(joda.DateTimeZone.UTC).getMillis

  @inline def millisToDuration(millis: Long): Duration = Duration.ofMillis(millis)
  @inline def millisFromDuration(d: Duration): Long =
    SECONDS.toMillis(d.getSeconds) + NANOSECONDS.toMillis(d.getNano.toLong)
  @inline def millisToJodaDuration(millis: Long): joda.Duration = joda.Duration.millis(millis)
  @inline def millisFromJodaDuration(d: joda.Duration): Long = d.getMillis

  // micros /////////////////////////////////////////////////////
  @inline def microsToInstant(microsFromEpoch: Long): Instant =
    unitToInstant(MICROSECONDS, microsFromEpoch)
  @inline def microsFromInstant(instant: Instant): Long =
    SECONDS.toMicros(instant.getEpochSecond) + NANOSECONDS.toMicros(instant.getNano.toLong)
  @inline def microsToJodaInstant(microsFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(MICROSECONDS.toMillis(microsFromEpoch))
  @inline def microsFromJodaInstant(instant: joda.Instant): Long =
    MILLISECONDS.toMicros(instant.getMillis)

  @inline def microsToJodaDateTime(microsFromEpoch: Long): joda.DateTime =
    new joda.DateTime(MICROSECONDS.toMillis(microsFromEpoch), joda.DateTimeZone.UTC)
  @inline def microsFromJodaDateTime(dt: joda.DateTime): Long =
    MILLISECONDS.toMicros(dt.getMillis)

  @inline def microsToLocalTime(microsFromMidnight: Long): LocalTime =
    LocalTime.ofNanoOfDay(MICROSECONDS.toNanos(microsFromMidnight))
  @inline def microsFromLocalTime(lt: LocalTime): Long =
    NANOSECONDS.toMicros(lt.toNanoOfDay)
  @inline def microsToJodaLocalTime(microsFromMidnight: Long): joda.LocalTime =
    joda.LocalTime.fromMillisOfDay(
      MICROSECONDS.toMillis(microsFromMidnight),
      ISOChronology.getInstanceUTC
    )
  @inline def microsFromJodaLocalTime(lt: joda.LocalTime): Long =
    MILLISECONDS.toMicros(lt.millisOfDay().get().toLong)

  @inline def microsToLocalDateTime(microsFromEpoch: Long): LocalDateTime =
    LocalDateTime.ofInstant(microsToInstant(microsFromEpoch), ZoneOffset.UTC)
  @inline def microsFromLocalDateTime(ldt: LocalDateTime): Long =
    microsFromInstant(ldt.toInstant(ZoneOffset.UTC))
  @inline def microsToJodaLocalDateTime(microsFromEpoch: Long): joda.LocalDateTime =
    new joda.LocalDateTime(MICROSECONDS.toMillis(microsFromEpoch), joda.DateTimeZone.UTC)
  @inline def microsFromJodaLocalDateTime(ldt: joda.LocalDateTime): Long =
    MILLISECONDS.toMicros(ldt.toDateTime(joda.DateTimeZone.UTC).getMillis)

  @inline def microsToDuration(micros: Long): Duration =
    Duration.ofMillis(MICROSECONDS.toMillis(micros))
  @inline def microsFromDuration(d: Duration): Long =
    SECONDS.toMicros(d.getSeconds) + NANOSECONDS.toMicros(d.getNano.toLong)
  @inline def microsToJodaDuration(micros: Long): joda.Duration =
    joda.Duration.millis(MICROSECONDS.toMillis(micros))
  @inline def microsFromJodaDuration(d: joda.Duration): Long =
    MILLISECONDS.toMicros(d.getMillis)

  // nanos /////////////////////////////////////////////////////
  // Long does not technically have enough range for Instant
  @inline def nanosToInstant(epochNanos: Long): Instant =
    unitToInstant(NANOSECONDS, epochNanos)
  @inline def nanosFromInstant(instant: Instant): Long =
    SECONDS.toNanos(instant.getEpochSecond) + instant.getNano
  @inline def nanosToJodaInstant(nanosFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(NANOSECONDS.toMillis(nanosFromEpoch))
  @inline def nanosFromJodaInstant(instant: joda.Instant): Long =
    MILLISECONDS.toNanos(instant.getMillis)

  @inline def nanosToJodaDateTime(nanosFromEpoch: Long): joda.DateTime =
    new joda.DateTime(NANOSECONDS.toMillis(nanosFromEpoch), joda.DateTimeZone.UTC)
  @inline def nanosFromJodaDateTime(dt: joda.DateTime): Long =
    MILLISECONDS.toNanos(dt.getMillis)

  @inline def nanosToLocalTime(nanosFromMidnight: Long): LocalTime =
    LocalTime.ofNanoOfDay(nanosFromMidnight)
  @inline def nanosFromLocalTime(lt: LocalTime): Long = lt.toNanoOfDay
  @inline def nanosToJodaLocalTime(nanosFromMidnight: Long): joda.LocalTime =
    joda.LocalTime.fromMillisOfDay(
      NANOSECONDS.toMillis(nanosFromMidnight),
      ISOChronology.getInstanceUTC
    )
  @inline def nanosFromJodaLocalTime(lt: joda.LocalTime): Long =
    MILLISECONDS.toNanos(lt.millisOfDay().get().toLong)

  @inline def nanosToLocalDateTime(nanosFromEpoch: Long): LocalDateTime =
    LocalDateTime.ofInstant(nanosToInstant(nanosFromEpoch), ZoneOffset.UTC)
  @inline def nanosFromLocalDateTime(ldt: LocalDateTime): Long =
    nanosFromInstant(ldt.toInstant(ZoneOffset.UTC))
  @inline def nanosToJodaLocalDateTime(nanosFromEpoch: Long): joda.LocalDateTime =
    new joda.LocalDateTime(NANOSECONDS.toMillis(nanosFromEpoch), joda.DateTimeZone.UTC)
  @inline def nanosFromJodaLocalDateTime(ldt: joda.LocalDateTime): Long =
    MILLISECONDS.toNanos(ldt.toDateTime(joda.DateTimeZone.UTC).getMillis)

  @inline def nanosToDuration(nanos: Long): Duration =
    Duration.ofNanos(nanos)
  @inline def nanosFromDuration(d: Duration): Long =
    SECONDS.toNanos(d.getSeconds) + d.getNano
  @inline def nanosToJodaDuration(nanos: Long): joda.Duration =
    joda.Duration.millis(NANOSECONDS.toMillis(nanos))
  @inline def nanosFromJodaDuration(d: joda.Duration): Long =
    MILLISECONDS.toNanos(d.getMillis)
}

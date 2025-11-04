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
      val remainderUnits = unitsFromEpoch - unit.convert(epochSeconds, SECONDS)
      val nanoAdjustment = unit.toNanos(remainderUnits)
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
  @inline def localDateToJodaLocalDate(ld: LocalDate): joda.LocalDate = {
    val epochDay = ld.getLong(ChronoField.EPOCH_DAY)
    val epochDayInt = Math.toIntExact(epochDay)
    EpochJodaDate.plusDays(epochDayInt)
  }
  @inline def jodaLocalTimeToLocalTime(t: joda.LocalTime): LocalTime = {
    val nanosLong = Math.multiplyExact(t.getMillisOfSecond.toLong, MILLISECONDS.toNanos(1L))
    val nanos = Math.toIntExact(nanosLong)
    LocalTime.of(t.getHourOfDay, t.getMinuteOfHour, t.getSecondOfMinute, nanos)
  }
  @inline def localTimeToJodaLocalTime(t: LocalTime): joda.LocalTime = {
    val millisLong = NANOSECONDS.toMillis(t.getNano.toLong)
    val millis = Math.toIntExact(millisLong)
    new joda.LocalTime(t.getHour, t.getMinute, t.getSecond, millis, ISOChronology.getInstanceUTC)
  }
  @inline def jodaLocalDateTimeToLocalDateTime(ldt: joda.LocalDateTime): LocalDateTime = {
    val nanosLong = Math.multiplyExact(ldt.getMillisOfSecond.toLong, MILLISECONDS.toNanos(1L))
    val nanos = Math.toIntExact(nanosLong)
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
    val millisInt = Math.toIntExact(millis)
    new joda.LocalDateTime(
      ldt.getYear,
      ldt.getMonthValue,
      ldt.getDayOfMonth,
      ldt.getHour,
      ldt.getMinute,
      ldt.getSecond,
      millisInt,
      ISOChronology.getInstanceUTC
    )
  }

  // millis /////////////////////////////////////////////////////
  @inline def millisToSecondsAndNanos(millis: Long): (Long, Long) = {
    val seconds = MILLISECONDS.toSeconds(millis)
    val remainderMillis = millis - Math.multiplyExact(seconds, SECONDS.toMillis(1L))
    val nanos = Math.multiplyExact(remainderMillis, MILLISECONDS.toNanos(1L))
    (seconds, nanos)
  }
  @inline def millisFromSecondsAndNanos(seconds: Long, nanos: Long): Long = {
    val millisFromSeconds = Math.multiplyExact(seconds, SECONDS.toMillis(1L))
    val millisFromNanos = NANOSECONDS.toMillis(nanos)
    Math.addExact(millisFromSeconds, millisFromNanos)
  }

  @inline def millisToInstant(millisFromEpoch: Long): Instant =
    Instant.ofEpochMilli(millisFromEpoch)
  @inline def millisFromInstant(instant: Instant): Long = instant.toEpochMilli
  @inline def millisToJodaInstant(millisFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(millisFromEpoch)
  @inline def millisFromJodaInstant(instant: joda.Instant): Long = instant.getMillis

  @inline def millisToLocalTime(millisFromMidnight: Int): LocalTime = {
    val nanos = Math.multiplyExact(millisFromMidnight.toLong, MILLISECONDS.toNanos(1L))
    LocalTime.ofNanoOfDay(nanos)
  }
  @inline def millisFromLocalTime(lt: LocalTime): Int = {
    val millisLong = NANOSECONDS.toMillis(lt.toNanoOfDay)
    Math.toIntExact(millisLong)
  }
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
  @inline def millisFromDuration(d: Duration): Long = {
    val millisFromSeconds = Math.multiplyExact(d.getSeconds, SECONDS.toMillis(1L))
    val millisFromNanos = NANOSECONDS.toMillis(d.getNano.toLong)
    Math.addExact(millisFromSeconds, millisFromNanos)
  }
  @inline def millisToJodaDuration(millis: Long): joda.Duration = joda.Duration.millis(millis)
  @inline def millisFromJodaDuration(d: joda.Duration): Long = d.getMillis

  // micros /////////////////////////////////////////////////////
  @inline def microsToInstant(microsFromEpoch: Long): Instant =
    unitToInstant(MICROSECONDS, microsFromEpoch)
  @inline def microsFromInstant(instant: Instant): Long = {
    val microsFromSeconds = Math.multiplyExact(instant.getEpochSecond, SECONDS.toMicros(1L))
    val microsFromNanos = NANOSECONDS.toMicros(instant.getNano.toLong)
    Math.addExact(microsFromSeconds, microsFromNanos)
  }
  @inline def microsToJodaInstant(microsFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(MICROSECONDS.toMillis(microsFromEpoch))
  @inline def microsFromJodaInstant(instant: joda.Instant): Long =
    Math.multiplyExact(instant.getMillis, MILLISECONDS.toMicros(1L))

  @inline def microsToJodaDateTime(microsFromEpoch: Long): joda.DateTime =
    new joda.DateTime(MICROSECONDS.toMillis(microsFromEpoch), joda.DateTimeZone.UTC)
  @inline def microsFromJodaDateTime(dt: joda.DateTime): Long =
    Math.multiplyExact(dt.getMillis, MILLISECONDS.toMicros(1L))

  @inline def microsToLocalTime(microsFromMidnight: Long): LocalTime = {
    val nanos = Math.multiplyExact(microsFromMidnight, MICROSECONDS.toNanos(1L))
    LocalTime.ofNanoOfDay(nanos)
  }
  @inline def microsFromLocalTime(lt: LocalTime): Long =
    NANOSECONDS.toMicros(lt.toNanoOfDay)
  @inline def microsToJodaLocalTime(microsFromMidnight: Long): joda.LocalTime =
    joda.LocalTime.fromMillisOfDay(
      MICROSECONDS.toMillis(microsFromMidnight),
      ISOChronology.getInstanceUTC
    )
  @inline def microsFromJodaLocalTime(lt: joda.LocalTime): Long =
    Math.multiplyExact(lt.millisOfDay().get().toLong, MILLISECONDS.toMicros(1L))

  @inline def microsToLocalDateTime(microsFromEpoch: Long): LocalDateTime =
    LocalDateTime.ofInstant(microsToInstant(microsFromEpoch), ZoneOffset.UTC)
  @inline def microsFromLocalDateTime(ldt: LocalDateTime): Long =
    microsFromInstant(ldt.toInstant(ZoneOffset.UTC))
  @inline def microsToJodaLocalDateTime(microsFromEpoch: Long): joda.LocalDateTime =
    new joda.LocalDateTime(MICROSECONDS.toMillis(microsFromEpoch), joda.DateTimeZone.UTC)
  @inline def microsFromJodaLocalDateTime(ldt: joda.LocalDateTime): Long =
    Math.multiplyExact(ldt.toDateTime(joda.DateTimeZone.UTC).getMillis, MILLISECONDS.toMicros(1L))

  @inline def microsToDuration(micros: Long): Duration = {
    val nanos = Math.multiplyExact(micros, MICROSECONDS.toNanos(1L))
    Duration.ofNanos(nanos)
  }
  @inline def microsFromDuration(d: Duration): Long = {
    val microsFromSeconds = Math.multiplyExact(d.getSeconds, SECONDS.toMicros(1L))
    val microsFromNanos = NANOSECONDS.toMicros(d.getNano.toLong)
    Math.addExact(microsFromSeconds, microsFromNanos)
  }
  @inline def microsToJodaDuration(micros: Long): joda.Duration =
    joda.Duration.millis(MICROSECONDS.toMillis(micros))
  @inline def microsFromJodaDuration(d: joda.Duration): Long =
    Math.multiplyExact(d.getMillis, MILLISECONDS.toMicros(1L))

  // nanos /////////////////////////////////////////////////////
  // Long does not technically have enough range for Instant
  @inline def nanosToInstant(epochNanos: Long): Instant =
    unitToInstant(NANOSECONDS, epochNanos)
  @inline def nanosFromInstant(instant: Instant): Long = {
    val nanosFromSeconds = Math.multiplyExact(instant.getEpochSecond, SECONDS.toNanos(1L))
    Math.addExact(nanosFromSeconds, instant.getNano.toLong)
  }
  @inline def nanosToJodaInstant(nanosFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(NANOSECONDS.toMillis(nanosFromEpoch))
  @inline def nanosFromJodaInstant(instant: joda.Instant): Long =
    Math.multiplyExact(instant.getMillis, MILLISECONDS.toNanos(1L))

  @inline def nanosToJodaDateTime(nanosFromEpoch: Long): joda.DateTime =
    new joda.DateTime(NANOSECONDS.toMillis(nanosFromEpoch), joda.DateTimeZone.UTC)
  @inline def nanosFromJodaDateTime(dt: joda.DateTime): Long =
    Math.multiplyExact(dt.getMillis, MILLISECONDS.toNanos(1L))

  @inline def nanosToLocalTime(nanosFromMidnight: Long): LocalTime =
    LocalTime.ofNanoOfDay(nanosFromMidnight)
  @inline def nanosFromLocalTime(lt: LocalTime): Long = lt.toNanoOfDay
  @inline def nanosToJodaLocalTime(nanosFromMidnight: Long): joda.LocalTime =
    joda.LocalTime.fromMillisOfDay(
      NANOSECONDS.toMillis(nanosFromMidnight),
      ISOChronology.getInstanceUTC
    )
  @inline def nanosFromJodaLocalTime(lt: joda.LocalTime): Long =
    Math.multiplyExact(lt.millisOfDay().get().toLong, MILLISECONDS.toNanos(1L))

  @inline def nanosToLocalDateTime(nanosFromEpoch: Long): LocalDateTime =
    LocalDateTime.ofInstant(nanosToInstant(nanosFromEpoch), ZoneOffset.UTC)
  @inline def nanosFromLocalDateTime(ldt: LocalDateTime): Long =
    nanosFromInstant(ldt.toInstant(ZoneOffset.UTC))
  @inline def nanosToJodaLocalDateTime(nanosFromEpoch: Long): joda.LocalDateTime =
    new joda.LocalDateTime(NANOSECONDS.toMillis(nanosFromEpoch), joda.DateTimeZone.UTC)
  @inline def nanosFromJodaLocalDateTime(ldt: joda.LocalDateTime): Long =
    Math.multiplyExact(ldt.toDateTime(joda.DateTimeZone.UTC).getMillis, MILLISECONDS.toNanos(1L))

  @inline def nanosToDuration(nanos: Long): Duration =
    Duration.ofNanos(nanos)
  @inline def nanosFromDuration(d: Duration): Long = {
    val nanosFromSeconds = Math.multiplyExact(d.getSeconds, SECONDS.toNanos(1L))
    Math.addExact(nanosFromSeconds, d.getNano.toLong)
  }
  @inline def nanosToJodaDuration(nanos: Long): joda.Duration =
    joda.Duration.millis(NANOSECONDS.toMillis(nanos))
  @inline def nanosFromJodaDuration(d: joda.Duration): Long =
    Math.multiplyExact(d.getMillis, MILLISECONDS.toNanos(1L))
}

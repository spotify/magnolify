package magnolify.shared

import org.joda.time as joda
import java.time.{Duration, Instant, LocalDateTime, LocalTime, ZoneOffset}
import java.util.concurrent.TimeUnit

object Time {
//  @inline def microsToMillis(micros: Long): Long = TimeUnit.MICROSECONDS.toMillis(micros)
//  @inline def millisToMicros(millis: Long): Long = TimeUnit.MILLISECONDS.toMicros(millis)

  // millis /////////////////////////////////////////////////////
  @inline def millisToInstant(millisFromEpoch: Long): Instant =
    Instant.ofEpochMilli(millisFromEpoch)
  @inline def millisFromInstant(instant: Instant): Long = instant.toEpochMilli
  @inline def millisToJodaInstant(millisFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(millisFromEpoch)
  @inline def millisFromJodaInstant(instant: joda.Instant): Long = instant.getMillis

  @inline def millisToLocalTime(millisFromMidnight: Int): LocalTime =
    LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(millisFromMidnight.toLong))
  @inline def millisFromLocalTime(lt: LocalTime): Int =
    TimeUnit.NANOSECONDS.toMillis(lt.toNanoOfDay).toInt
  @inline def millisToJodaLocalTime(millisFromMidnight: Int): joda.LocalTime =
    joda.LocalTime.fromMillisOfDay(millisFromMidnight.toLong)
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
    TimeUnit.SECONDS.toMillis(d.getSeconds) + TimeUnit.NANOSECONDS.toMillis(d.getNano.toLong)
  @inline def millisToJodaDuration(millis: Long): joda.Duration = joda.Duration.millis(millis)
  @inline def millisFromJodaDuration(d: joda.Duration): Long = d.getMillis

  // micros /////////////////////////////////////////////////////
  @inline def microsToInstant(microsFromEpoch: Long): Instant = {
    val epochSeconds = TimeUnit.MICROSECONDS.toSeconds(microsFromEpoch)
    val nanoAdjustment = TimeUnit.MICROSECONDS.toNanos(microsFromEpoch % 1_000_000L)
    Instant.ofEpochSecond(epochSeconds, nanoAdjustment)
  }
  @inline def microsFromInstant(instant: Instant): Long = {
    val seconds = instant.getEpochSecond
    val nanos = instant.getNano
    if (seconds < 0 && nanos > 0) {
      val micros = Math.multiplyExact(seconds + 1, 1000000L)
      val adjustment = (nanos / 1000L) - 1000000
      Math.addExact(micros, adjustment)
    } else {
      val micros = Math.multiplyExact(seconds, 1000000L)
      Math.addExact(micros, nanos / 1000L)
    }
  }
  @inline def microsToJodaInstant(microsFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(TimeUnit.MICROSECONDS.toMillis(microsFromEpoch))
  @inline def microsFromJodaInstant(instant: joda.Instant): Long =
    TimeUnit.MILLISECONDS.toMicros(instant.getMillis)

  @inline def microsToJodaDateTime(microsFromEpoch: Long): joda.DateTime =
    new joda.DateTime(TimeUnit.MICROSECONDS.toMillis(microsFromEpoch), joda.DateTimeZone.UTC)
  @inline def microsFromJodaDateTime(dt: joda.DateTime): Long =
    TimeUnit.MILLISECONDS.toMicros(dt.getMillis)

  @inline def microsToLocalTime(microsFromMidnight: Long): LocalTime =
    LocalTime.ofNanoOfDay(TimeUnit.MICROSECONDS.toNanos(microsFromMidnight))
  @inline def microsFromLocalTime(lt: LocalTime): Long =
    TimeUnit.NANOSECONDS.toMicros(lt.toNanoOfDay)
  @inline def microsToJodaLocalTime(microsFromMidnight: Long): joda.LocalTime =
    joda.LocalTime.fromMillisOfDay(TimeUnit.MICROSECONDS.toMillis(microsFromMidnight))
  @inline def microsFromJodaLocalTime(lt: joda.LocalTime): Long =
    TimeUnit.MILLISECONDS.toMicros(lt.millisOfDay().get().toLong)

  @inline def microsToLocalDateTime(microsFromEpoch: Long): LocalDateTime =
    LocalDateTime.ofInstant(microsToInstant(microsFromEpoch), ZoneOffset.UTC)
  @inline def microsFromLocalDateTime(ldt: LocalDateTime): Long =
    microsFromInstant(ldt.toInstant(ZoneOffset.UTC))

  @inline def microsToDuration(micros: Long): Duration =
    Duration.ofMillis(TimeUnit.MICROSECONDS.toMillis(micros))
  @inline def microsFromDuration(d: Duration): Long =
    TimeUnit.SECONDS.toMicros(d.getSeconds) + TimeUnit.NANOSECONDS.toMicros(d.getNano.toLong)
  @inline def microsToJodaDuration(micros: Long): joda.Duration =
    joda.Duration.millis(TimeUnit.MICROSECONDS.toMillis(micros))
  @inline def microsFromJodaDuration(d: joda.Duration): Long =
    TimeUnit.MILLISECONDS.toMicros(d.getMillis)

  // nanos /////////////////////////////////////////////////////
  // Long does not technically have enough range for Instant
  @inline def nanosToInstant(epochNanos: Long): Instant =
    Instant.ofEpochSecond(TimeUnit.NANOSECONDS.toSeconds(epochNanos), epochNanos % 1_000_000_000L)
  @inline def nanosFromInstant(instant: Instant): Long =
    TimeUnit.SECONDS.toNanos(instant.getEpochSecond) + instant.getNano
  @inline def nanosToJodaInstant(nanosFromEpoch: Long): joda.Instant =
    joda.Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(nanosFromEpoch))
  @inline def nanosFromJodaInstant(instant: joda.Instant): Long =
    TimeUnit.MILLISECONDS.toNanos(instant.getMillis)

  @inline def nanosToJodaDateTime(nanosFromEpoch: Long): joda.DateTime =
    new joda.DateTime(TimeUnit.NANOSECONDS.toMillis(nanosFromEpoch), joda.DateTimeZone.UTC)
  @inline def nanosFromJodaDateTime(dt: joda.DateTime): Long =
    TimeUnit.MILLISECONDS.toNanos(dt.getMillis)

  @inline def nanosToLocalTime(nanosFromMidnight: Long): LocalTime =
    LocalTime.ofNanoOfDay(nanosFromMidnight)
  @inline def nanosFromLocalTime(lt: LocalTime): Long = lt.toNanoOfDay
  @inline def nanosToJodaLocalTime(nanosFromMidnight: Long): joda.LocalTime =
    joda.LocalTime.fromMillisOfDay(TimeUnit.NANOSECONDS.toMillis(nanosFromMidnight))
  @inline def nanosFromJodaLocalTime(lt: joda.LocalTime): Long =
    TimeUnit.MILLISECONDS.toNanos(lt.millisOfDay().get().toLong)

  @inline def nanosToLocalDateTime(nanosFromEpoch: Long): LocalDateTime =
    LocalDateTime.ofInstant(nanosToInstant(nanosFromEpoch), ZoneOffset.UTC)
  @inline def nanosFromLocalDateTime(ldt: LocalDateTime): Long =
    nanosFromInstant(ldt.toInstant(ZoneOffset.UTC))

  @inline def nanosToDuration(nanos: Long): Duration =
    Duration.ofNanos(nanos)
  @inline def nanosFromDuration(d: Duration): Long =
    TimeUnit.SECONDS.toNanos(d.getSeconds) + d.getNano
  @inline def nanosToJodaDuration(nanos: Long): joda.Duration =
    joda.Duration.millis(TimeUnit.NANOSECONDS.toMillis(nanos))
  @inline def nanosFromJodaDuration(d: joda.Duration): Long =
    TimeUnit.MILLISECONDS.toNanos(d.getMillis)
}

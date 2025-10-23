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
import org.scalacheck.{Arbitrary, Gen}

import java.time.{Duration, Instant, LocalDate, LocalDateTime, LocalTime, OffsetTime, ZoneOffset}
import java.util.concurrent.TimeUnit

trait TimeArbitrary {
  implicit lazy val arbInstant: Arbitrary[Instant] =
    Arbitrary(Gen.posNum[Long].map(Instant.ofEpochMilli))
  implicit lazy val arbLocalDate: Arbitrary[LocalDate] =
    Arbitrary(Gen.chooseNum(0L, 365L * 100).map(LocalDate.ofEpochDay))
  implicit lazy val arbLocalTime: Arbitrary[LocalTime] =
    Arbitrary(arbInstant.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalTime))
  implicit lazy val arbLocalDateTime: Arbitrary[LocalDateTime] =
    Arbitrary(arbInstant.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalDateTime))
  implicit lazy val arbOffsetTime: Arbitrary[OffsetTime] =
    Arbitrary(arbInstant.arbitrary.map(_.atOffset(ZoneOffset.UTC).toOffsetTime))
  implicit lazy val arbDuration: Arbitrary[Duration] =
    Arbitrary(Gen.posNum[Long].map(Duration.ofMillis))

  implicit val arbJodaDate: Arbitrary[joda.LocalDate] = Arbitrary {
    Arbitrary.arbitrary[LocalDate].map { ld =>
      new joda.LocalDate(ld.getYear, ld.getMonthValue, ld.getDayOfMonth)
    }
  }
  implicit val arbJodaDateTime: Arbitrary[joda.DateTime] = Arbitrary {
    Arbitrary.arbitrary[Instant].map { i =>
      new joda.DateTime(i.toEpochMilli, joda.DateTimeZone.UTC)
    }
  }
  implicit val arbJodaLocalTime: Arbitrary[joda.LocalTime] = Arbitrary {
    Arbitrary.arbitrary[LocalTime].map { lt =>
      joda.LocalTime.fromMillisOfDay(TimeUnit.NANOSECONDS.toMillis(lt.toNanoOfDay))
    }
  }
  implicit val arbJodaLocalDateTime: Arbitrary[joda.LocalDateTime] = Arbitrary {
    Arbitrary.arbitrary[LocalDateTime].map { ldt =>
      joda.LocalDateTime.parse(ldt.toString)
    }
  }
  implicit val arbJodaDuration: Arbitrary[joda.Duration] =
    Arbitrary(Gen.posNum[Long].map(joda.Duration.millis))
  implicit val arbJodaInstant: Arbitrary[joda.Instant] =
    Arbitrary(Gen.posNum[Long].map(l => new joda.Instant(l)))

}

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

package magnolify.test

import java.time._

import cats._
import cats.instances.all._
import org.scalacheck._

object Time {
  implicit val arbInstant: Arbitrary[Instant] =
    Arbitrary(Gen.chooseNum[Long](0, Int.MaxValue).map(Instant.ofEpochMilli))
  implicit val arbLocalDate: Arbitrary[LocalDate] =
    Arbitrary(Gen.chooseNum(0L, 365L * 100).map(LocalDate.ofEpochDay))
  implicit val arbLocalTime: Arbitrary[LocalTime] =
    Arbitrary(arbInstant.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalTime))
  implicit val arbLocalDateTime: Arbitrary[LocalDateTime] =
    Arbitrary(arbInstant.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalDateTime))
  implicit val arbOffsetTime: Arbitrary[OffsetTime] =
    Arbitrary(arbInstant.arbitrary.map(_.atOffset(ZoneOffset.UTC).toOffsetTime))

  implicit val eqInstant: Eq[Instant] = Eq.by(_.toEpochMilli)
  implicit val eqLocalDate: Eq[LocalDate] = Eq.by(_.toEpochDay)
  implicit val eqLocalTime: Eq[LocalTime] = Eq.by(_.toNanoOfDay)
  implicit val eqLocalDateTime: Eq[LocalDateTime] = Eq.by(_.toEpochSecond(ZoneOffset.UTC))
  implicit val eqOffsetTime: Eq[OffsetTime] = Eq.by(_.toLocalTime.toNanoOfDay)
}

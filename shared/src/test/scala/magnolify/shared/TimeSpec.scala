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
import org.scalacheck.*
import org.scalacheck.Prop.forAll

class TimeSpec extends Properties("Time") with TimeArbitrary {
  import Time._

  case class Convert[T, U: Arbitrary, V: Arbitrary](
    name: String,
    javaTo: T => U,
    javaFrom: U => T,
    jodaTo: T => V,
    jodaFrom: V => T
  ) {
    def java =
      property(name) = forAll((u: U) => (javaFrom andThen javaTo)(u) == u)
    def joda =
      property(s"$name-joda") = forAll((v: V) => (jodaFrom andThen jodaTo)(v) == v)
    def roundtrip =
      property(s"$name-roundtrip") =
        forAll((u: U) => (javaFrom andThen jodaTo andThen jodaFrom andThen javaTo)(u) == u)
  }

  val conversions: List[Convert[?, ?, ?]] = List(
    Convert(
      "millis-instant",
      millisToInstant,
      millisFromInstant,
      millisToJodaInstant,
      millisFromJodaInstant
    ),
    Convert(
      "millis-localtime",
      millisToLocalTime,
      millisFromLocalTime,
      millisToJodaLocalTime,
      millisFromJodaLocalTime
    ),
    Convert(
      "millis-localdatetime",
      millisToLocalDateTime,
      millisFromLocalDateTime,
      millisToJodaLocalDateTime,
      millisFromJodaLocalDateTime
    ),
    Convert(
      "millis-duration",
      millisToDuration,
      millisFromDuration,
      millisToJodaDuration,
      millisFromJodaDuration
    ),
    Convert(
      "micros-instant",
      microsToInstant,
      microsFromInstant,
      microsToJodaInstant,
      microsFromJodaInstant
    ),
    Convert(
      "micros-localtime",
      microsToLocalTime,
      microsFromLocalTime,
      microsToJodaLocalTime,
      microsFromJodaLocalTime
    ),
    Convert(
      "micros-localdatetime",
      microsToLocalDateTime,
      microsFromLocalDateTime,
      microsToJodaLocalDateTime,
      microsFromJodaLocalDateTime
    ),
    Convert(
      "micros-duration",
      microsToDuration,
      microsFromDuration,
      microsToJodaDuration,
      microsFromJodaDuration
    ),
    Convert(
      "nanos-instant",
      nanosToInstant,
      nanosFromInstant,
      nanosToJodaInstant,
      nanosFromJodaInstant
    ),
    Convert(
      "nanos-localtime",
      nanosToLocalTime,
      nanosFromLocalTime,
      nanosToJodaLocalTime,
      nanosFromJodaLocalTime
    ),
    Convert(
      "nanos-localdatetime",
      nanosToLocalDateTime,
      nanosFromLocalDateTime,
      nanosToJodaLocalDateTime,
      nanosFromJodaLocalDateTime
    ),
    Convert(
      "nanos-duration",
      nanosToDuration,
      nanosFromDuration,
      nanosToJodaDuration,
      nanosFromJodaDuration
    )
  )

  conversions.foreach { c =>
    c.java
    c.joda
    c.roundtrip
  }

  property(s"millis-datetime-joda") =
    forAll((v: joda.DateTime) => (millisFromJodaDateTime _ andThen millisToJodaDateTime)(v) == v)
  property(s"micros-datetime-joda") =
    forAll((v: joda.DateTime) => (microsFromJodaDateTime _ andThen microsToJodaDateTime)(v) == v)
  property(s"nanos-datetime-joda") =
    forAll((v: joda.DateTime) => (nanosFromJodaDateTime _ andThen nanosToJodaDateTime)(v) == v)
}

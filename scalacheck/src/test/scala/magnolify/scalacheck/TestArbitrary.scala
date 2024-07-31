/*
 * Copyright 2022 Spotify AB
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

package magnolify.scalacheck

import magnolify.scalacheck.semiauto.*
import magnolify.shared.UnsafeEnum
import magnolify.test.ADT.*
import magnolify.test.JavaEnums
import magnolify.test.Simple.*
import org.joda.time as joda
import org.scalacheck.*
import org.scalacheck.rng.Seed

import java.net.URI
import java.nio.ByteBuffer
import java.time.*

object TestArbitrary {
  // seed
  implicit lazy val arbSeed: Arbitrary[Seed] = Arbitrary(
    Arbitrary.arbLong.arbitrary.map(Seed.apply)
  )

  // null
  implicit lazy val arbNull: Arbitrary[Null] = Arbitrary(Gen.const(null))

  // java
  implicit lazy val arbCharSequence: Arbitrary[CharSequence] = Arbitrary {
    Gen.listOf(Gen.asciiChar).map { cs =>
      val sb = new StringBuilder()
      sb.appendAll(cs)
      sb
    }
  }
  implicit val arbByteBuffer: Arbitrary[ByteBuffer] = Arbitrary {
    Arbitrary.arbitrary[Array[Byte]].map(ByteBuffer.wrap)
  }

  // java-time
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

  // joda-time
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
      joda.LocalTime.fromMillisOfDay(lt.toNanoOfDay / 1000)
    }
  }
  implicit val arbJodaLocalDateTime: Arbitrary[joda.LocalDateTime] = Arbitrary {
    Arbitrary.arbitrary[LocalDateTime].map { ldt =>
      joda.LocalDateTime.parse(ldt.toString)
    }
  }

  // enum
  implicit lazy val arbJavaEnum: Arbitrary[JavaEnums.Color] =
    Arbitrary(Gen.oneOf(JavaEnums.Color.values.toSeq))
  implicit lazy val arbScalaEnums: Arbitrary[ScalaEnums.Color.Type] =
    Arbitrary(Gen.oneOf(ScalaEnums.Color.values))
  implicit def arbUnsafeEnum[T](implicit arb: Arbitrary[T]): Arbitrary[UnsafeEnum[T]] = Arbitrary {
    Gen.oneOf(
      arb.arbitrary.map(UnsafeEnum.Known.apply),
      Gen.alphaNumStr.suchThat(_.nonEmpty).map(UnsafeEnum.Unknown.apply)
    )
  }

  // ADT
  implicit lazy val arbNode: Arbitrary[Node] = Arbitrary.gen[Node]
  implicit lazy val arbGNode: Arbitrary[GNode[Int]] = Arbitrary.gen[GNode[Int]]
  implicit lazy val arbShape: Arbitrary[Shape] = Arbitrary.gen[Shape]
  implicit lazy val arbColor: Arbitrary[Color] = Arbitrary.gen[Color]
  implicit lazy val arbPerson: Arbitrary[Person] = Arbitrary.gen[Person]

  // simple
  implicit lazy val arbIntegers: Arbitrary[Integers] = Arbitrary.gen[Integers]
  implicit lazy val arbFloats: Arbitrary[Floats] = Arbitrary.gen[Floats]
  implicit lazy val arbNumbers: Arbitrary[Numbers] = Arbitrary.gen[Numbers]
  implicit lazy val arbRequired: Arbitrary[Required] = Arbitrary.gen[Required]
  implicit lazy val arbNullable: Arbitrary[Nullable] = Arbitrary.gen[Nullable]
  implicit lazy val arbRepeated: Arbitrary[Repeated] = Arbitrary.gen[Repeated]
  implicit lazy val arbNested: Arbitrary[Nested] = Arbitrary.gen[Nested]
  implicit lazy val arbCollections: Arbitrary[Collections] = Arbitrary.gen[Collections]
  implicit lazy val arbMoreCollections: Arbitrary[MoreCollections] =
    Arbitrary.gen[MoreCollections]
  implicit lazy val arbEnums: Arbitrary[Enums] = Arbitrary.gen[Enums]
  implicit lazy val arbUnsafeEnums: Arbitrary[UnsafeEnums] = Arbitrary.gen[UnsafeEnums]
  implicit lazy val arbCustom: Arbitrary[Custom] = Arbitrary.gen[Custom]
  implicit lazy val arbLowerCamel: Arbitrary[LowerCamel] = Arbitrary.gen[LowerCamel]
  implicit lazy val arbLowerCamelInner: Arbitrary[LowerCamelInner] =
    Arbitrary.gen[LowerCamelInner]

  // other
  implicit lazy val arbUri: Arbitrary[URI] = Arbitrary(Gen.alphaNumStr.map(URI.create))
}

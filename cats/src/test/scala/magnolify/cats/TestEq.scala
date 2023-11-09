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

package magnolify.cats

import cats.Eq
import magnolify.cats.semiauto.*
import magnolify.shared.UnsafeEnum
import magnolify.test.ADT._
import magnolify.test.JavaEnums
import magnolify.test.Simple._
import org.joda.{time => joda}

import java.net.URI
import java.nio.ByteBuffer
import java.time._

object TestEq {

  // other
  implicit lazy val eqNull: Eq[Null] = Eq.allEqual
  implicit lazy val eqUri: Eq[URI] = Eq.fromUniversalEquals
  implicit def eqArray[T: Eq]: Eq[Array[T]] =
    Eq.by(_.toList)
  implicit def eqIterable[T: Eq, C[_]](implicit ti: C[T] => Iterable[T]): Eq[C[T]] =
    Eq.by[C[T], List[T]](ti(_).toList)(Eq.catsKernelEqForList[T])

  // java
  implicit lazy val eqCharSequence: Eq[CharSequence] = Eq.by(_.toString)

  // Map[CharSequence, T] should not be used for lookups as key equality is not guarantee
  // Can only be used as a key value list
  implicit def eqCharSeqMap[T: Eq]: Eq[Map[CharSequence, T]] =
    Eq.by[Map[CharSequence, T], Map[String, T]](
      _.map { case (k, v) => k.toString -> v }
    )(Eq.catsKernelEqForMap[String, T])

  implicit val eqByteBuffer: Eq[ByteBuffer] = Eq.by(_.array())

  // java-time
  implicit lazy val eqInstant: Eq[Instant] = Eq.fromUniversalEquals
  implicit lazy val eqLocalDate: Eq[LocalDate] = Eq.fromUniversalEquals
  implicit lazy val eqLocalTime: Eq[LocalTime] = Eq.fromUniversalEquals
  implicit lazy val eqLocalDateTime: Eq[LocalDateTime] = Eq.fromUniversalEquals
  implicit lazy val eqOffsetTime: Eq[OffsetTime] = Eq.fromUniversalEquals
  implicit lazy val eqDuration: Eq[Duration] = Eq.fromUniversalEquals

  // joda-time
  implicit val eqJodaDate: Eq[joda.LocalDate] = Eq.fromUniversalEquals
  implicit val eqJodaDateTime: Eq[joda.DateTime] = Eq.fromUniversalEquals
  implicit val eqJodaLocalTime: Eq[joda.LocalTime] = Eq.fromUniversalEquals
  implicit val eqJodaLocalDateTime: Eq[joda.LocalDateTime] = Eq.fromUniversalEquals

  // enum
  implicit lazy val eqJavaEnum: Eq[JavaEnums.Color] = Eq.fromUniversalEquals
  implicit lazy val eqScalaEnum: Eq[ScalaEnums.Color.Type] = Eq.fromUniversalEquals
  implicit def eqUnsafeEnum[T](implicit eq: Eq[T]): Eq[UnsafeEnum[T]] = Eq.instance {
    case (UnsafeEnum.Known(x), UnsafeEnum.Known(y))     => eq.eqv(x, y)
    case (UnsafeEnum.Unknown(x), UnsafeEnum.Unknown(y)) => x == y
    case _                                              => false
  }

  // ADT
  implicit lazy val eqNode: Eq[Node] = Eq.gen[Node]
  implicit lazy val eqGNode: Eq[GNode[Int]] = Eq.gen[GNode[Int]]
  implicit lazy val eqShape: Eq[Shape] = Eq.gen[Shape]
  implicit lazy val eqColor: Eq[Color] = Eq.gen[Color]
  implicit lazy val eqPerson: Eq[Person] = Eq.gen[Person]

  // simple
  implicit lazy val eqIntegers: Eq[Integers] = Eq.gen[Integers]
  implicit lazy val eqFloats: Eq[Floats] = Eq.gen[Floats]
  implicit lazy val eqNumbers: Eq[Numbers] = Eq.gen[Numbers]
  implicit lazy val eqRequired: Eq[Required] = Eq.gen[Required]
  implicit lazy val eqNullable: Eq[Nullable] = Eq.gen[Nullable]
  implicit lazy val eqRepeated: Eq[Repeated] = Eq.gen[Repeated]
  implicit lazy val eqNested: Eq[Nested] = Eq.gen[Nested]
  implicit lazy val eqCollections: Eq[Collections] = Eq.gen[Collections]
  implicit lazy val eqMoreCollections: Eq[MoreCollections] = Eq.gen[MoreCollections]
  implicit lazy val eqEnums: Eq[Enums] = Eq.gen[Enums]
  implicit lazy val eqUnsafeEnums: Eq[UnsafeEnums] = Eq.gen[UnsafeEnums]
  implicit lazy val eqCustom: Eq[Custom] = Eq.gen[Custom]
  implicit lazy val eqLowerCamel: Eq[LowerCamel] = Eq.gen[LowerCamel]
  implicit lazy val eqLowerCamelInner: Eq[LowerCamelInner] = Eq.gen[LowerCamelInner]

}

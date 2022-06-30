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

package magnolify.cats.test

import cats.Eq
import magnolify.cats.semiauto.EqDerivation
import magnolify.test.ADT._
import magnolify.test.JavaEnums
import magnolify.test.Simple._
import magnolify.shared.UnsafeEnum

import java.net.URI
import java.time._

object TestEqImplicits {

  // other
  implicit lazy val eqUri: Eq[URI] = Eq.fromUniversalEquals
  implicit lazy val eqArray: Eq[Array[Int]] = Eq.by(_.toList)
  implicit def eqIterable[T, C[_]](implicit eq: Eq[T], ti: C[T] => Iterable[T]): Eq[C[T]] =
    Eq.instance { (x, y) =>
      val xs = ti(x)
      val ys = ti(y)
      xs.size == ys.size && (xs zip ys).forall((eq.eqv _).tupled)
    }

  // time
  implicit lazy val eqInstant: Eq[Instant] = Eq.by(_.toEpochMilli)
  implicit lazy val eqLocalDate: Eq[LocalDate] = Eq.by(_.toEpochDay)
  implicit lazy val eqLocalTime: Eq[LocalTime] = Eq.by(_.toNanoOfDay)
  implicit lazy val eqLocalDateTime: Eq[LocalDateTime] = Eq.by(_.toEpochSecond(ZoneOffset.UTC))
  implicit lazy val eqOffsetTime: Eq[OffsetTime] = Eq.by(_.toLocalTime.toNanoOfDay)
  implicit lazy val eqDuration: Eq[Duration] = Eq.by(_.toMillis)

  // enum
  implicit lazy val eqJavaEnum: Eq[JavaEnums.Color] = Eq.fromUniversalEquals
  implicit lazy val eqScalaEnum: Eq[ScalaEnums.Color.Type] = Eq.fromUniversalEquals
  implicit def eqUnsafeEnum[T](implicit eq: Eq[T]): Eq[UnsafeEnum[T]] = Eq.instance {
    case (UnsafeEnum.Known(x), UnsafeEnum.Known(y))     => eq.eqv(x, y)
    case (UnsafeEnum.Unknown(x), UnsafeEnum.Unknown(y)) => x == y
    case _                                              => false
  }

  // ADT
  implicit lazy val eqNode: Eq[Node] = EqDerivation[Node]
  implicit lazy val eqGNode: Eq[GNode[Int]] = EqDerivation[GNode[Int]]
  implicit lazy val eqShape: Eq[Shape] = EqDerivation[Shape]
  implicit lazy val eqColor: Eq[Color] = EqDerivation[Color]
  implicit lazy val eqPerson: Eq[Person] = EqDerivation[Person]

  // simple
  implicit lazy val eqIntegers: Eq[Integers] = EqDerivation[Integers]
  implicit lazy val eqFloats: Eq[Floats] = EqDerivation[Floats]
  implicit lazy val eqNumbers: Eq[Numbers] = EqDerivation[Numbers]
  implicit lazy val eqRequired: Eq[Required] = EqDerivation[Required]
  implicit lazy val eqNullable: Eq[Nullable] = EqDerivation[Nullable]
  implicit lazy val eqRepeated: Eq[Repeated] = EqDerivation[Repeated]
  implicit lazy val eqNested: Eq[Nested] = EqDerivation[Nested]
  implicit lazy val eqCollections: Eq[Collections] = EqDerivation[Collections]
  implicit lazy val eqMoreCollections: Eq[MoreCollections] = EqDerivation[MoreCollections]
  implicit lazy val eqEnums: Eq[Enums] = EqDerivation[Enums]
  implicit lazy val eqUnsafeEnums: Eq[UnsafeEnums] = EqDerivation[UnsafeEnums]
  implicit lazy val eqCustom: Eq[Custom] = EqDerivation[Custom]
  implicit lazy val eqLowerCamel: Eq[LowerCamel] = EqDerivation[LowerCamel]
  implicit lazy val eqLowerCamelInner: Eq[LowerCamelInner] = EqDerivation[LowerCamelInner]

}

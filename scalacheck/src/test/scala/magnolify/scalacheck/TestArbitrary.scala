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

import magnolify.scalacheck.semiauto.ArbitraryDerivation
import magnolify.shared.{TimeArbitrary, UnsafeEnum}
import magnolify.test.ADT.*
import magnolify.test.JavaEnums
import magnolify.test.Simple.*
import org.scalacheck.*

import java.net.URI
import java.nio.ByteBuffer

object TestArbitrary extends TimeArbitrary {
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
  implicit lazy val arbNode: Arbitrary[Node] = ArbitraryDerivation[Node]
  implicit lazy val arbGNode: Arbitrary[GNode[Int]] = ArbitraryDerivation[GNode[Int]]
  implicit lazy val arbShape: Arbitrary[Shape] = ArbitraryDerivation[Shape]
  implicit lazy val arbColor: Arbitrary[Color] = ArbitraryDerivation[Color]
  implicit lazy val arbPerson: Arbitrary[Person] = ArbitraryDerivation[Person]

  // simple
  implicit lazy val arbIntegers: Arbitrary[Integers] = ArbitraryDerivation[Integers]
  implicit lazy val arbFloats: Arbitrary[Floats] = ArbitraryDerivation[Floats]
  implicit lazy val arbNumbers: Arbitrary[Numbers] = ArbitraryDerivation[Numbers]
  implicit lazy val arbRequired: Arbitrary[Required] = ArbitraryDerivation[Required]
  implicit lazy val arbNullable: Arbitrary[Nullable] = ArbitraryDerivation[Nullable]
  implicit lazy val arbRepeated: Arbitrary[Repeated] = ArbitraryDerivation[Repeated]
  implicit lazy val arbNested: Arbitrary[Nested] = ArbitraryDerivation[Nested]
  implicit lazy val arbCollections: Arbitrary[Collections] = ArbitraryDerivation[Collections]
  implicit lazy val arbMoreCollections: Arbitrary[MoreCollections] =
    ArbitraryDerivation[MoreCollections]
  implicit lazy val arbEnums: Arbitrary[Enums] = ArbitraryDerivation[Enums]
  implicit lazy val arbUnsafeEnums: Arbitrary[UnsafeEnums] = ArbitraryDerivation[UnsafeEnums]
  implicit lazy val arbCustom: Arbitrary[Custom] = ArbitraryDerivation[Custom]
  implicit lazy val arbLowerCamel: Arbitrary[LowerCamel] = ArbitraryDerivation[LowerCamel]
  implicit lazy val arbLowerCamelInner: Arbitrary[LowerCamelInner] =
    ArbitraryDerivation[LowerCamelInner]

  // other
  implicit lazy val arbUri: Arbitrary[URI] = Arbitrary(Gen.alphaNumStr.map(URI.create))
}

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

package magnolify.bigtable.test

import java.net.URI
import java.time.Duration
import java.util.UUID

import cats._
import com.google.bigtable.v2.Row
import com.google.protobuf.ByteString
import magnolify.bigtable._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shared.CaseMapper
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class BigtableTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit t: BigtableType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    property(className[T]) {
      Prop.forAll { t: T =>
        val mutations = tpe(t, "cf")
        val row = BigtableType.mutationsToRow(ByteString.EMPTY, mutations)
        val copy = tpe(row, "cf")
        val rowCopy =
          BigtableType.mutationsToRow(ByteString.EMPTY, BigtableType.rowToMutations(row))

        Prop.all(
          eq.eqv(t, copy),
          row == rowCopy
        )
      }
    }
  }

  import magnolify.scalacheck.test.TestArbitrary._
  import magnolify.cats.test.TestEq._
  import magnolify.shared.TestEnumType._
  implicit val arbByteString: Arbitrary[ByteString] =
    Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
  implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
  implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
  implicit val btfUri: BigtableField[URI] =
    BigtableField.from[String](x => URI.create(x))(_.toString)
  implicit val btfDuration: BigtableField[Duration] =
    BigtableField.from[Long](Duration.ofMillis)(_.toMillis)

  test[Numbers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[BigtableNested]
  test[Collections]
  test[MoreCollections]
  test[Enums]
  test[UnsafeEnums]
  test[Custom]
  test[BigtableTypes]

  test("DefaultInner") {
    val bt = ensureSerializable(BigtableType[DefaultInner])
    assertEquals(bt(Row.getDefaultInstance, "cf"), DefaultInner())
    val inner = DefaultInner(2, Some(2))
    assertEquals(bt(BigtableType.mutationsToRow(ByteString.EMPTY, bt(inner, "cf")), "cf"), inner)
  }

  test("DefaultOuter") {
    val bt = ensureSerializable(BigtableType[DefaultOuter])
    assertEquals(bt(Row.getDefaultInstance, "cf"), DefaultOuter())
    val outer = DefaultOuter(DefaultInner(3, Some(3)), Some(DefaultInner(3, Some(3))))
    assertEquals(bt(BigtableType.mutationsToRow(ByteString.EMPTY, bt(outer, "cf")), "cf"), outer)
  }

  test("LowerCamel mapping") {
    implicit val bt: BigtableType[LowerCamel] = BigtableType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    val fields = LowerCamel.fields
      .map(_.toUpperCase)
      .map(l => if (l == "INNERFIELD") "INNERFIELD.INNERFIRST" else l)
    val record = bt(LowerCamel.default, "cf")
    assertEquals(record.map(_.getSetCell.getColumnQualifier.toStringUtf8), fields)
  }
}

// Collections are not supported
case class BigtableNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])

case class BigtableTypes(b: Byte, c: Char, s: Short, bs: ByteString, ba: Array[Byte], uu: UUID)

// Collections are not supported
case class DefaultInner(i: Int = 1, o: Option[Int] = Some(1))
case class DefaultOuter(
  i: DefaultInner = DefaultInner(2, Some(2)),
  o: Option[DefaultInner] = Some(DefaultInner(2, Some(2)))
)

/*
 * Copyright 2020 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.protobuf.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import com.google.protobuf.{ByteString, Message}
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.protobuf._
import magnolify.protobuf.unsafe._
import magnolify.shared.CaseMapper
import magnolify.shims.JavaConverters._
import magnolify.test.Proto2._
import magnolify.test.Proto3._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class ProtobufTypeSuite extends MagnolifySuite {
  private def test[T: ClassTag: Arbitrary, U <: Message: ClassTag](implicit
    t: ProtobufType[T, U],
    eqt: Eq[T]
  ): Unit = {
    val tpe = ensureSerializable(t)

    property(s"${className[T]}.${className[U]}") {
      Prop.forAll { t: T =>
        val r = tpe(t)
        val copy = tpe(r)
        eqt.eqv(t, copy)
      }
    }
  }

  test[Integers, IntegersP2]
  test[Integers, IntegersP3]
  test[Floats, FloatsP2]
  test[Floats, FloatsP3]
  test[Required, RequiredP2]
  test[Required, SingularP3]
  test[Nullable, NullableP2]

  // PROTO3 removes the notion of require vs optional fields.
  // By default `Option[T] are not supported`.
  test("Fail PROTO3 Option[T]") {
    val msg = "requirement failed: Option[T] support is PROTO2 only, " +
      "`import magnolify.protobuf.unsafe.Proto3Option._` to enable PROTO3 support"
    interceptMessage[IllegalArgumentException](msg)(ProtobufType[Nullable, SingularP3])
  }

  // Adding `import magnolify.protobuf.unsafe.Proto3Option._` enables PROTO3 `Option[T]` support.
  // The new singular field returns default value if unset.
  // Hence `None` round trips back as `Some(false/0/"")`.
  {
    import magnolify.protobuf.unsafe.Proto3Option._
    val eq: Eq[Nullable] = Eq.by { x =>
      Required(x.b.getOrElse(false), x.i.getOrElse(0), x.s.getOrElse(""))
    }
    val arb: Arbitrary[Nullable] = implicitly[Arbitrary[Nullable]]
    test(classTag[Nullable], arb, classTag[SingularP3], ProtobufType[Nullable, SingularP3], eq)
  }

  test[Repeated, RepeatedP2]
  test[Repeated, RepeatedP3]
  test[Nested, NestedP2]
  test[NestedNoOption, NestedP3]
  test[UnsafeByte, IntegersP2]
  test[UnsafeChar, IntegersP2]
  test[UnsafeShort, IntegersP2]

  {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[BytesA, BytesP2]
    test[BytesB, BytesP3]
  }

  {
    import Collections._
    test[Collections, CollectionP2]
    test[MoreCollections, MoreCollectionP2]
    test[Collections, CollectionP3]
    test[MoreCollections, MoreCollectionP3]
  }

  {
    import Custom._
    implicit val pfUri: ProtobufField[URI] = ProtobufField.from[String](URI.create)(_.toString)
    implicit val pfDuration: ProtobufField[Duration] =
      ProtobufField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom, CustomP2]
    test[Custom, CustomP3]
  }

  {
    implicit val pt: ProtobufType[LowerCamel, UpperCaseP3] =
      ProtobufType[LowerCamel, UpperCaseP3](CaseMapper(_.toUpperCase))
    test[LowerCamel, UpperCaseP3]
  }
}

case class UnsafeByte(i: Byte, l: Long)
case class UnsafeChar(i: Char, l: Long)
case class UnsafeShort(i: Short, l: Long)
case class BytesA(b: ByteString)
case class BytesB(b: Array[Byte])
case class NestedNoOption(
  b: Boolean,
  i: Int,
  s: String,
  r: Required,
  l: List[Required]
)

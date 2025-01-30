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

package magnolify.protobuf

import java.net.URI
import java.time.Duration

import cats._
import com.google.protobuf.{ByteString, Message}
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.protobuf._
import magnolify.protobuf.Proto2._
import magnolify.protobuf.Proto3._
import magnolify.protobuf.unsafe._
import magnolify.shared._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._
import magnolify.scalacheck.TestArbitrary._
import magnolify.cats.TestEq.{eqEnums => _, eqNullable => _, _}

import scala.reflect._

trait BaseProtobufTypeSuite extends MagnolifySuite {
  def test[T: ClassTag: Arbitrary, U <: Message: ClassTag](implicit
    t: ProtobufType[T, U],
    eqt: Eq[T]
  ): Unit = {
    val tpe = ensureSerializable(t)

    property(s"${className[T]}.${className[U]}") {
      Prop.forAll { (t: T) =>
        val r = tpe(t)
        val copy = tpe(r)
        eqt.eqv(t, copy)
      }
    }
  }
}

class ProtobufTypeSuite extends BaseProtobufTypeSuite {

  test[Integers, IntegersP2]
  test[Integers, IntegersP3]
  test[Floats, FloatsP2]
  test[Floats, FloatsP3]
  test[Required, RequiredP2]
  test[Required, RequiredP3]
  test[Nullable, NullableP2]
  test[Nullable, NullableP3]

  test[Repeated, RepeatedP2]
  test[Repeated, RepeatedP3]
  test[Nested, NestedP2]
  test[Nested, NestedP3]
  test[UnsafeByte, IntegersP2]
  test[UnsafeChar, IntegersP2]
  test[UnsafeShort, IntegersP2]

  test[Collections, CollectionsP2]
  test[Collections, CollectionsP3]
  test[MoreCollections, MoreCollectionsP2]
  test[MoreCollections, MoreCollectionsP3]

  test[Maps, MapsP2]
  test[Maps, MapsP3]

  test("AnyVal") {
    test[ProtoHasValueClass, IntegersP2]
    test[ProtoHasValueClass, IntegersP3]
  }
}

// Workaround for "Method too large: magnolify/protobuf/test/ProtobufTypeSuite.<init> ()V"
class MoreProtobufTypeSuite extends BaseProtobufTypeSuite {

  implicit val arbByteString: Arbitrary[ByteString] =
    Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
  implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
  implicit val pfUri: ProtobufField[URI] = ProtobufField.from[String](URI.create)(_.toString)
  implicit val pfDuration: ProtobufField[Duration] =
    ProtobufField.from[Long](Duration.ofMillis)(_.toMillis)

  test[BytesA, BytesP2]
  test[BytesB, BytesP3]

  {
    import Proto2Enums._
    test[Enums, EnumsP2]
    test[UnsafeEnums, UnsafeEnumsP2]
  }

  {
    import Proto3Enums._
    test[Enums, EnumsP3]
    test[UnsafeEnums, UnsafeEnumsP3]
  }

  test[Custom, CustomP2]
  test[Custom, CustomP3]

  {
    implicit val pt: ProtobufType[LowerCamel, UpperCaseP3] =
      ProtobufType[LowerCamel, UpperCaseP3](CaseMapper(_.toUpperCase))
    test[LowerCamel, UpperCaseP3]
  }

  {
    import Proto2Enums._
    test[DefaultsRequired2, DefaultRequiredP2]
    test[DefaultsNullable2, DefaultNullableP2]
  }

  {
    import Proto3Enums._
    test[DefaultIntegers3, IntegersP3]
    test[DefaultFloats3, FloatsP3]
    test[DefaultRequired3, RequiredP3]
    test[DefaultEnums3, EnumsP3]
  }

  {
    test[DefaultNullable3, NullableP3]
  }

  {
    import Proto2Enums._
    type F[T] = ProtobufType[T, _]
    testFail[F, DefaultMismatch2](ProtobufType[DefaultMismatch2, DefaultRequiredP2])(
      "Default mismatch magnolify.protobuf.DefaultMismatch2#i: 321 != 123"
    )
    testFail[F, DefaultMismatch3](ProtobufType[DefaultMismatch3, RequiredP3])(
      "Default mismatch magnolify.protobuf.DefaultMismatch3#i: 321 != 0"
    )
  }
}

object Proto2Enums {
  implicit val efJavaEnum2: ProtobufField[JavaEnums.Color] =
    ProtobufField.enum[JavaEnums.Color, EnumsP2.JavaEnums]
  implicit val efScalaEnum2: ProtobufField[ScalaEnums.Color.Type] =
    ProtobufField.enum[ScalaEnums.Color.Type, EnumsP2.ScalaEnums]
  implicit val efAdtEnum2: ProtobufField[ADT.Color] =
    ProtobufField.enum[ADT.Color, EnumsP2.ScalaEnums]
}

object Proto3Enums {
  implicit val efJavaEnum3: ProtobufField[JavaEnums.Color] =
    ProtobufField.enum[JavaEnums.Color, EnumsP3.JavaEnums]
  implicit val efScalaEnum3: ProtobufField[ScalaEnums.Color.Type] =
    ProtobufField.enum[ScalaEnums.Color.Type, EnumsP3.ScalaEnums]
  implicit val efAdtEnum3: ProtobufField[ADT.Color] =
    ProtobufField.enum[ADT.Color, EnumsP3.ScalaEnums]
}

case class Maps(mp: Map[String, Int], mn: Map[String, Nested])
case class ProtoValueClass(value: Long) extends AnyVal
case class ProtoHasValueClass(i: Int, l: ProtoValueClass)
case class UnsafeByte(i: Byte, l: Long)
case class UnsafeChar(i: Char, l: Long)
case class UnsafeShort(i: Short, l: Long)
case class BytesA(b: ByteString)
case class BytesB(b: Array[Byte])

case class DefaultsRequired2(
  i: Int = 123,
  l: Long = 456L,
  f: Float = 1.23f,
  d: Double = 4.56,
  b: Boolean = true,
  s: String = "abc",
  bs: ByteString = ByteString.copyFromUtf8("def"),
  je: JavaEnums.Color = JavaEnums.Color.GREEN,
  se: ScalaEnums.Color.Type = ScalaEnums.Color.Green,
  ae: ADT.Color = ADT.Green
)

case class DefaultsNullable2(
  i: Option[Int] = Some(123),
  l: Option[Long] = Some(456L),
  f: Option[Float] = Some(1.23f),
  d: Option[Double] = Some(4.56),
  b: Option[Boolean] = Some(true),
  s: Option[String] = Some("abc"),
  bs: Option[ByteString] = Some(ByteString.copyFromUtf8("def")),
  je: Option[JavaEnums.Color] = Some(JavaEnums.Color.GREEN),
  se: Option[ScalaEnums.Color.Type] = Some(ScalaEnums.Color.Green),
  ae: Option[ADT.Color] = Some(ADT.Green)
)

case class DefaultIntegers3(i: Int = 0, l: Long = 0)
case class DefaultFloats3(f: Float = 0, d: Double = 0)
case class DefaultRequired3(b: Boolean = false, s: String = "", i: Int = 0)
case class DefaultNullable3(
  b: Option[Boolean] = Some(false),
  s: Option[String] = Some(""),
  i: Option[Int] = Some(0)
)
case class DefaultEnums3(
  j: JavaEnums.Color = JavaEnums.Color.RED,
  s: ScalaEnums.Color.Type = ScalaEnums.Color.Red,
  a: ADT.Color = ADT.Red
)

case class DefaultMismatch2(
  i: Int = 321,
  l: Long = 456L,
  f: Float = 1.23f,
  d: Double = 4.56,
  b: Boolean = true,
  s: String = "abc",
  bs: ByteString = ByteString.copyFromUtf8("def"),
  je: JavaEnums.Color = JavaEnums.Color.GREEN,
  se: ScalaEnums.Color.Type = ScalaEnums.Color.Green,
  ae: ADT.Color = ADT.Green
)
case class DefaultMismatch3(i: Int = 321, l: Long = 0)

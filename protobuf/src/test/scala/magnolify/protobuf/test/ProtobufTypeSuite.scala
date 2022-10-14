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

package magnolify.protobuf.test

import java.net.URI
import java.time.Duration

import cats._
import com.google.protobuf.{ByteString, Message}
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.protobuf._
import magnolify.protobuf.unsafe._
import magnolify.shared._
import magnolify.test.Proto2._
import magnolify.test.Proto3._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

trait BaseProtobufTypeSuite extends MagnolifySuite {
  def test[T: ClassTag: Arbitrary, U <: Message: ClassTag](implicit
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
}

class ProtobufTypeSuite extends BaseProtobufTypeSuite {
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
    implicit val eq: Eq[Nullable] = Eq.by { x =>
      Required(x.b.getOrElse(false), x.i.getOrElse(0), x.s.getOrElse(""))
    }
    test[Nullable, SingularP3]
  }

  test[Repeated, RepeatedP2]
  test[Repeated, RepeatedP3]
  test[Nested, NestedP2]
  test[NestedNoOption, NestedP3]
  test[UnsafeByte, IntegersP2]
  test[UnsafeChar, IntegersP2]
  test[UnsafeShort, IntegersP2]

  {
    import Collections._
    test[Collections, CollectionP2]
    test[MoreCollections, MoreCollectionP2]
    test[Collections, CollectionP3]
    test[MoreCollections, MoreCollectionP3]
  }

  test("AnyVal") {
    test[ProtoHasValueClass, IntegersP2]
  }
}

// Workaround for "Method too large: magnolify/protobuf/test/ProtobufTypeSuite.<init> ()V"
class MoreProtobufTypeSuite extends BaseProtobufTypeSuite {
  {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[BytesA, BytesP2]
    test[BytesB, BytesP3]
  }

  {
    import Enums._
    import UnsafeEnums._
    import Proto2Enums._
    test[Enums, EnumsP2]
    test[UnsafeEnums, UnsafeEnumsP2]
  }

  {
    import Enums._
    import UnsafeEnums._
    import Proto3Enums._
    import magnolify.protobuf.unsafe.Proto3Option._
    // Enums are encoded as integers and default to zero value
    implicit val eq: Eq[Enums] = Eq.by(e =>
      (
        e.j,
        e.s,
        e.a,
        e.jo.getOrElse(JavaEnums.Color.RED),
        e.so.getOrElse(ScalaEnums.Color.Red),
        e.ao.getOrElse(ADT.Red),
        e.jr,
        e.sr,
        e.ar
      )
    )
    test[Enums, EnumsP3]
    // Unsafe enums are encoded as string and default "" is treated as None
    test[UnsafeEnums, UnsafeEnumsP3]
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

  {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    import Enums._

    {
      import Proto2Enums._
      test[DefaultsRequired2, DefaultRequiredP2]
      test[DefaultsNullable2, DefaultNullableP2]
    }

    {
      import Proto3Enums._
      test[DefaultIntegers3, IntegersP3]
      test[DefaultFloats3, FloatsP3]
      test[DefaultRequired3, SingularP3]
      test[DefaultEnums3, EnumsP3]
    }
  }

  {
    import magnolify.protobuf.unsafe.Proto3Option._
    val eq: Eq[DefaultNullable3] = Eq.by { x =>
      Required(x.b.getOrElse(false), x.i.getOrElse(0), x.s.getOrElse(""))
    }
    val arb: Arbitrary[DefaultNullable3] = implicitly[Arbitrary[DefaultNullable3]]
    val pt = ProtobufType[DefaultNullable3, SingularP3]
    test(classTag[DefaultNullable3], arb, classTag[SingularP3], pt, eq)
  }

  {
    import Enums._
    import Proto2Enums._
    type F[T] = ProtobufType[T, _]
    testFail[F, DefaultMismatch2](ProtobufType[DefaultMismatch2, DefaultRequiredP2])(
      "Default mismatch magnolify.protobuf.test.DefaultMismatch2#i: 321 != 123"
    )
    testFail[F, DefaultMismatch3](ProtobufType[DefaultMismatch3, SingularP3])(
      "Default mismatch magnolify.protobuf.test.DefaultMismatch3#i: 321 != 0"
    )
  }
}

object Proto2Enums {
  // FIXME: for some reasons these implicits fail to resolve without explicit types
  implicit val efJavaEnum2: ProtobufField[JavaEnums.Color] =
    ProtobufField.enum[JavaEnums.Color, EnumsP2.JavaEnums]
  implicit val efScalaEnum2: ProtobufField[ScalaEnums.Color.Type] =
    ProtobufField.enum[ScalaEnums.Color.Type, EnumsP2.ScalaEnums]
  implicit val efAdtEnum2: ProtobufField[ADT.Color] =
    ProtobufField.enum[ADT.Color, EnumsP2.ScalaEnums]
}

object Proto3Enums {
  // FIXME: for some reasons these implicits fail to resolve without explicit types
  implicit val efJavaEnum3: ProtobufField[JavaEnums.Color] =
    ProtobufField.enum[JavaEnums.Color, EnumsP3.JavaEnums]
  implicit val efScalaEnum3: ProtobufField[ScalaEnums.Color.Type] =
    ProtobufField.enum[ScalaEnums.Color.Type, EnumsP3.ScalaEnums]
  implicit val efAdtEnum3: ProtobufField[ADT.Color] =
    ProtobufField.enum[ADT.Color, EnumsP3.ScalaEnums]
}

case class ProtoValueClass(value: Long) extends AnyVal
case class ProtoHasValueClass(i: Int, l: ProtoValueClass)
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

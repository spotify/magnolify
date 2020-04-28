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
import magnolify.shims.JavaConverters._
import magnolify.test.Proto2._
import magnolify.test.Proto3._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object ProtobufTypeSpec extends MagnolifySpec("ProtobufType") {
  {
    val t = ensureSerializable(ProtobufType[Repeated, RepeatedP2])
    t(RepeatedP2.getDefaultInstance)
  }
  private def test[T: ClassTag: Arbitrary: Eq, U <: Message: ClassTag](
    implicit t: ProtobufType[T, U],
    eqt: Eq[T]
  ): Unit = {
    val tpe = ensureSerializable(t)

    property(className[U]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy = tpe(r)
      eqt.eqv(t, copy)
    }
  }

  test[Integers, IntegersP2]
  test[Integers, IntegersP3]
  // PROTO3 removes the notion of require vs optional fields.
  // The new singular field returns default value if unset, making it required essentially.
  test[Required, RequiredP2]
  test[Required, SingularP3]
  test[Nullable, NullableP2]
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

  try {
    val pt = ProtobufType[NullableNoneValue, RequiredP2]
    pt(RequiredP2.getDefaultInstance)
  } catch {
    case e: IllegalArgumentException =>
      require(e.getMessage == "requirement failed: @noneValue annotation supports PROTO3 only")
  }

  {
    val pt = ProtobufType[NullableNoneValue, SingularP3]
    ensureSerializable(pt)
    val defaults = pt(SingularP3.getDefaultInstance)
    val nones = pt(SingularP3.newBuilder().setB(true).setI(1).setS("abc").build())
    require(defaults == NullableNoneValue(Some(false), Some(0), Some("")))
    require(nones == NullableNoneValue(None, None, None))
  }

  {
    val pt = ProtobufType[NestedNoneValue, NestedP3]
    ensureSerializable(pt)
    val defaults = pt(NestedP3.getDefaultInstance)
    val nones = pt(
      NestedP3.newBuilder().setR(SingularP3.newBuilder().setB(true).setI(1).setS("abc")).build()
    )
    require(defaults == NestedNoneValue(false, 0, "", Some(Required(false, 0, "")), Nil))
    require(nones == NestedNoneValue(false, 0, "", None, Nil))
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

case class NullableNoneValue(
  @noneValue(true) b: Option[Boolean],
  @noneValue(1) i: Option[Int],
  @noneValue("abc") s: Option[String]
)
case class NestedNoneValue(
  b: Boolean,
  i: Int,
  s: String,
  @noneValue(Required(true, 1, "abc")) r: Option[Required],
  l: List[Required]
)

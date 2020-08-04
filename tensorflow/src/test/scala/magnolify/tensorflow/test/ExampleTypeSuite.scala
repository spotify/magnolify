/*
 * Copyright 2019 Spotify AB.
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
package magnolify.tensorflow.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import com.google.protobuf.ByteString
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shared.CaseMapper
import magnolify.shims.JavaConverters._
import magnolify.tensorflow._
import magnolify.tensorflow.unsafe._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class ExampleTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit t: ExampleType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    property(className[T]) {
      Prop.forAll { t: T =>
        val r = tpe(t)
        val copy = tpe(r)
        eq.eqv(t, copy)
      }
    }
  }

  // workaround for Double to Float precision loss
  implicit val arbDouble: Arbitrary[Double] =
    Arbitrary(Arbitrary.arbFloat.arbitrary.map(_.toDouble))

  test[Integers]
  test[Floats]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[ExampleNested]
  test[Unsafe]

  {
    import Collections._
    test[Collections]
    test[MoreCollections]
  }

  {
    import Custom._
    implicit val efUri: ExampleField.Primitive[URI] =
      ExampleField.from[ByteString](x => URI.create(x.toStringUtf8))(x =>
        ByteString.copyFromUtf8(x.toString)
      )
    implicit val efDuration: ExampleField.Primitive[Duration] =
      ExampleField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom]
  }

  {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[ExampleTypes]
  }

  {
    implicit val et: ExampleType[LowerCamel] = ExampleType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    test("LowerCamel mapping") {
      val fields = LowerCamel.fields
        .map(_.toUpperCase)
        .map(l => if (l == "INNERFIELD") "INNERFIELD.INNERFIRST" else l)
      val record = et(LowerCamel.default)
      assertEquals(record.getFeatures.getFeatureMap.keySet().asScala.toSet, fields.toSet)
    }
  }
}

// Option[T] and Seq[T] not supported
case class ExampleNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])
case class ExampleTypes(f: Float, bs: ByteString, ba: Array[Byte])

case class Unsafe(b: Byte, c: Char, s: Short, i: Int, d: Double, bool: Boolean, str: String)

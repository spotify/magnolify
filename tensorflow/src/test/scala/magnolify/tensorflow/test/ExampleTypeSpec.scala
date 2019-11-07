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
import magnolify.tensorflow._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object ExampleTypeSpec extends MagnolifySpec("ExampleType") {
  private def test[T: Arbitrary: ClassTag](implicit tpe: ExampleType[T], eq: Eq[T]): Unit = {
    ensureSerializable(tpe)
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy = tpe(r)
      eq.eqv(t, copy)
    }
  }

  implicit val efInt: ExampleField.Primitive[Int] = ExampleField.from[Long](_.toInt)(_.toLong)

  {
    implicit val efBoolean: ExampleField.Primitive[Boolean] =
      ExampleField.from[Long](_ == 1)(x => if (x) 1 else 0)
    implicit val efString: ExampleField.Primitive[String] =
      ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8)
    test[Integers]
    test[Required]
    test[Nullable]
    test[Repeated]
    test[ExampleNested]
  }

  {
    import Collections._
    test[Collections]
    test[MoreCollections]
  }

  {
    import Custom._
    implicit val efUri: ExampleField.Primitive[URI] =
      ExampleField.from[ByteString](x => URI.create(x.toStringUtf8))(
        x => ByteString.copyFromUtf8(x.toString)
      )
    implicit val efDuration: ExampleField.Primitive[Duration] =
      ExampleField.from[Long](Duration.ofMillis)(_.toMillis)

    test[Custom]
  }

  {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    test[ExampleTypes]
  }

  {
    implicit val efInt: ExampleField[Int] = ExampleField.from[Long](_.toInt)(_.toLong)
  }
}

// Option[T] and Seq[T] not supported
case class ExampleNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])

case class ExampleTypes(f: Float, bs: ByteString)

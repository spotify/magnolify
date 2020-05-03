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
package magnolify.bigtable.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import com.google.protobuf.ByteString
import magnolify.bigtable._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object BigtableTypeSpec extends MagnolifySpec("BigtableType") {
  private def test[T: Arbitrary: ClassTag](implicit t: BigtableType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    property(className[T]) = Prop.forAll { t: T =>
      val mutations = tpe(t, "cf")
      val row = BigtableType.mutationsToRow(ByteString.EMPTY, mutations)
      val copy = tpe(row, "cf")
      val rowCopy = BigtableType.mutationsToRow(ByteString.EMPTY, BigtableType.rowToMutations(row))

      Prop.all(
        eq.eqv(t, copy),
        row == rowCopy
      )
    }
  }

  test[Numbers]
  test[Required]
  test[Nullable]
  test[BigtableNested]

  {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[BigtableTypes]
  }

  {
    import Custom._
    implicit val btfUri: BigtableField[URI] =
      BigtableField.from[String](x => URI.create(x))(_.toString)
    implicit val btfDuration: BigtableField[Duration] =
      BigtableField.from[Long](Duration.ofMillis)(_.toMillis)

    test[Custom]
  }
}

// Collections are not supported
case class BigtableNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])

case class BigtableTypes(b: Byte, c: Char, s: Short, bs: ByteString, ba: Array[Byte])

/*
 * Copyright 2023 Spotify AB
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

import cats.Eq
import magnolify.cats.TestEq.*
import magnolify.cats.auto.*
import magnolify.hbase.{HbaseField, HbaseType}
import magnolify.scalacheck.TestArbitrary.*
import magnolify.scalacheck.auto.*
import magnolify.test.MagnolifySuite
import magnolify.test.Simple.*
import org.apache.hadoop.hbase.client.Result
import org.scalacheck.{Arbitrary, Prop}

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

class HbaseTypeSuite extends MagnolifySuite {

  private val row: Array[Byte] = "row".getBytes(UTF_8)
  private val family: Array[Byte] = "family".getBytes(UTF_8)

  private def test[T: Arbitrary: ClassTag](implicit hbt: HbaseType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(hbt)
    property(className[T]) {
      Prop.forAll { (t: T) =>
        val mutation = tpe(t, row, family)
        val cells = mutation.getFamilyCellMap.asScala.values
          .flatMap(_.asScala)
          .toArray
        val result = Result.create(cells)
        val copy = tpe(result, family)

        Prop.all(eq.eqv(t, copy))
      }
    }
  }

  implicit val eqByteArray: Eq[Array[Byte]] =
    Eq.by(_.toList)
  implicit val hbfUri: HbaseField[URI] =
    HbaseField.from[String](x => URI.create(x))(_.toString)
  implicit val hbfDuration: HbaseField[Duration] =
    HbaseField.from[Long](Duration.ofMillis)(_.toMillis)

  test[Numbers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[HbaseNested]
  test[Collections]
  test[MoreCollections]
  test[Enums]
  test[UnsafeEnums]
  test[Custom]
  test[HbaseTypes]

}

// Collections are not supported
case class HbaseNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])

case class HbaseTypes(b: Byte, c: Char, s: Short, ba: Array[Byte], uu: UUID)

// Collections are not supported
case class DefaultInner(i: Int = 1, o: Option[Int] = Some(1))
case class DefaultOuter(
  i: DefaultInner = DefaultInner(2, Some(2)),
  o: Option[DefaultInner] = Some(DefaultInner(2, Some(2)))
)

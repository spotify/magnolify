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

package magnolify.bigtable
package test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import com.google.protobuf.ByteString
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._
import com.google.bigtable.v2.Mutation
import com.google.cloud.bigtable.data.v2.models.{Row, RowCell}
import magnolify.shims.JavaConverters._

import scala.reflect._

// Collections are not supported
case class BigtableNested(b: Boolean, i: Int, s: String, r: Required, o: Option[Required])

object BigtableTypeSpec extends MagnolifySpec("BigtableType") {

  case class Inner(long: Long, str: String, uri: URI)
  case class Outer(inner: Inner)
  val record = Outer(Inner(1L, "hello", URI.create("https://www.spotify.com")))

  private def mutationsToRow(mutations: Iterable[Mutation]): Row =
    Row.create(
      ByteString.copyFromUtf8("key"),
      mutations.toList
        .map(_.getSetCell)
        .sortBy(_.getColumnQualifier.toStringUtf8)
        .map { setCell =>
        RowCell.create(
          setCell.getFamilyName, setCell.getColumnQualifier,
          setCell.getTimestampMicros, List.empty.asJava, setCell.getValue)
      }.asJava
  )

  private def test[T: Arbitrary: ClassTag]
  (implicit tpe: BigtableType[T], eq: Eq[T]): Unit = {
    ensureSerializable(tpe)
    property(className[T]) = Prop.forAll { t: T =>
      val mutations = tpe.to(t)
      val row = mutationsToRow(mutations)

      val copy = tpe.from(row)
      eq.eqv(t, copy)
    }
  }

  test[Int]
  test[String]
  test[Boolean]
  test[Double]
  test[Integers]
  test[Required]
  test[Nullable]
  test[BigtableNested]

  import Custom._

  implicit val efUri: BigtableField.Primitive[URI] =
    BigtableField.from[String](x => URI.create(x))(_.toString)

  implicit val efDuration: BigtableField.Primitive[Duration] =
    BigtableField.from[Long](Duration.ofMillis)(_.toMillis)

  test[Custom]
}

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
package magnolify.datastore.test

import java.net.URI
import java.time.{Duration, Instant}

import cats._
import cats.instances.all._
import com.google.datastore.v1.Entity
import com.google.datastore.v1.client.DatastoreHelper.makeValue
import com.google.protobuf.ByteString
import magnolify.datastore._
import magnolify.datastore.unsafe._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shared.CaseMapper
import magnolify.shims.JavaConverters._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object EntityTypeSpec extends MagnolifySpec("EntityType") {
  private def test[T: Arbitrary: ClassTag](implicit t: EntityType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy = tpe(r)
      eq.eqv(t, copy)
    }
  }

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Unsafe]

  {
    import Collections._
    test[Collections]
    test[MoreCollections]
  }

  {
    import Custom._
    implicit val efUri: EntityField[URI] = EntityField.from[String](URI.create)(_.toString)
    implicit val efDuration: EntityField[Duration] =
      EntityField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom]
  }

  {
    implicit val arbInstant: Arbitrary[Instant] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Instant.ofEpochMilli(_)))
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqInstant: Eq[Instant] = Eq.by(_.toEpochMilli)
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[DatastoreTypes]
  }

  {
    implicit val efInt: EntityField[Int] =
      EntityField.at[Int](_.getIntegerValue.toInt)(makeValue(_))
    implicit val efUri: EntityField[URI] = EntityField.from[String](URI.create)(_.toString)
  }

  {
    val it = EntityType[DefaultInner]
    require(it(Entity.getDefaultInstance) == DefaultInner())
    val inner = DefaultInner(2, Some(2), List(2, 2))
    require(it(it(inner)) == inner)

    val ot = EntityType[DefaultOuter]
    require(ot(Entity.getDefaultInstance) == DefaultOuter())
    val outer =
      DefaultOuter(DefaultInner(3, Some(3), List(3, 3)), Some(DefaultInner(3, Some(3), List(3, 3))))
    require(ot(ot(outer)) == outer)
  }

  {
    implicit val at = EntityType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    val fields = LowerCamel.fields.map(_.toUpperCase)

    val record = at(LowerCamel.default)
    require(record.getPropertiesMap.keySet().asScala == fields.toSet)
    require(record.getPropertiesOrThrow("INNERFIELD").getEntityValue.getPropertiesMap.containsKey("INNERFIRST"))
  }
}

case class Unsafe(b: Byte, c: Char, s: Short, i: Int, f: Float)
case class DatastoreTypes(u: Unit, bs: ByteString, ba: Array[Byte], ts: Instant)

case class DefaultInner(i: Int = 1, o: Option[Int] = Some(1), l: List[Int] = List(1, 1))
case class DefaultOuter(
  i: DefaultInner = DefaultInner(2, Some(2), List(2, 2)),
  o: Option[DefaultInner] = Some(DefaultInner(2, Some(2), List(2, 2)))
)

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
import com.google.datastore.v1.{Entity, Key}
import com.google.datastore.v1.client.DatastoreHelper.makeValue
import com.google.protobuf.ByteString
import magnolify.datastore._
import magnolify.datastore.unsafe._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shared.CaseMapper
import magnolify.shims.JavaConverters._
import magnolify.test.Simple._
import magnolify.test.Time._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class EntityTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit t: EntityType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    property(className[T]) {
      Prop.forAll { t: T =>
        val r = tpe(t)
        val copy = tpe(r)
        eq.eqv(t, copy)
      }
    }
  }

  test[Integers]
  test[Floats]
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
    import Enums._
    test[Enums]
  }

  {
    import Custom._
    implicit val efUri: EntityField[URI] = EntityField.from[String](URI.create)(_.toString)
    implicit val efDuration: EntityField[Duration] =
      EntityField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom]
  }

  {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[DatastoreTypes]
  }

  {
    implicit val efInt: EntityField[Int] =
      EntityField.at[Int](_.getIntegerValue.toInt)(makeValue(_))
    implicit val efUri: EntityField[URI] = EntityField.from[String](URI.create)(_.toString)
  }

  test("DefaultInner") {
    val et = ensureSerializable(EntityType[DefaultInner])
    assertEquals(et(Entity.getDefaultInstance), DefaultInner())
    val inner = DefaultInner(2, Some(2), List(2, 2))
    assertEquals(et(et(inner)), inner)
  }

  test("DefaultOuter") {
    val et = ensureSerializable(EntityType[DefaultOuter])
    assertEquals(et(Entity.getDefaultInstance), DefaultOuter())
    val outer =
      DefaultOuter(DefaultInner(3, Some(3), List(3, 3)), Some(DefaultInner(3, Some(3), List(3, 3))))
    assertEquals(et(et(outer)), outer)
  }

  def testKey[T: ClassTag, K](
    t: T,
    project: String,
    namespace: String,
    kind: String,
    kf: Key.PathElement => K,
    k: K
  )(implicit et: EntityType[T]): Unit = {
    test(s"@key ${className[T]}") {
      val record = et(t)
      assertEquals(record.getKey.getPartitionId.getProjectId, project)
      assertEquals(record.getKey.getPartitionId.getNamespaceId, namespace)
      val path = record.getKey.getPathList.asScala.toSeq
      assertEquals(path.map(_.getKind), Seq(kind))
      assertEquals(path.map(kf), Seq(k))
    }
  }

  {
    val ns = "magnolify.datastore.test"
    testKey(LongKey(123L), "", ns, "LongKey", _.getId, 123L)
    testKey(StringKey("abc"), "", ns, "StringKey", _.getName, "abc")
    testKey(InstantKey(Instant.ofEpochMilli(123L)), "", ns, "InstantKey", _.getId, 123L)
    testKey(CustomKey(123L), "my-project", "com.spotify", "MyKind", _.getId, 123L)

    testKey(IntKey(123), "", ns, "IntKey", _.getId, 123L)

    implicit val efUri: EntityField[URI] = EntityField.from[String](URI.create)(_.toString)
    testKey(UriKey(URI.create("spotify.com")), "", ns, "UriKey", _.getName, "spotify.com")

    implicit val kfByteString: KeyField[ByteString] = KeyField.at[ByteString](_.toStringUtf8)
    testKey(
      ByteStringKey(ByteString.copyFromUtf8("abc")),
      "",
      ns,
      "ByteStringKey",
      _.getName,
      "abc"
    )
    implicit val kfByteArray: KeyField[Array[Byte]] = KeyField.at[Array[Byte]](new String(_))
    testKey(ByteArrayKey("abc".getBytes), "", ns, "ByteArrayKey", _.getName, "abc")
    implicit val kfRecord: KeyField[RecordKey] = KeyField.at[RecordKey](_.l)
    testKey(NestedKey(RecordKey(123L, "abc")), "", ns, "NestedKey", _.getId, 123L)
  }

  test("@excludeFromIndexes") {
    val et = EntityType[EntityIndex]
    val ei = EntityIndex("foo", "bar", "baz", "zoo")
    val record = et(ei)
    assertEquals(et(record), ei)

    assert(!record.getPropertiesOrThrow("default").getExcludeFromIndexes)
    assert(record.getPropertiesOrThrow("excludedDefault").getExcludeFromIndexes)
    assert(record.getPropertiesOrThrow("excluded").getExcludeFromIndexes)
    assert(!record.getPropertiesOrThrow("included").getExcludeFromIndexes)
  }

  testFail(EntityType[DoubleKey])(
    "More than one @key annotation: magnolify.datastore.test.DoubleKey#k"
  )
  testFail(EntityType[MultiKey])(
    "More than one field with @key annotation: magnolify.datastore.test.MultiKey#[k1, k2]"
  )
  testFail(EntityType[NestedKey])("No KeyField[T] instance: magnolify.datastore.test.NestedKey#k")
  testFail(EntityType[DoubleEntityIndex])(
    "More than one @excludeFromIndexes annotation: magnolify.datastore.test.DoubleEntityIndex#i"
  )

  {
    implicit val et: EntityType[LowerCamel] = EntityType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    test("LowerCamel mapping") {
      val fields = LowerCamel.fields.map(_.toUpperCase)
      val record = et(LowerCamel.default)
      assertEquals(record.getPropertiesMap.keySet().asScala.toSet, fields.toSet)
      assert(
        record
          .getPropertiesOrThrow("INNERFIELD")
          .getEntityValue
          .getPropertiesMap
          .containsKey("INNERFIRST")
      )
    }
  }
}

case class Unsafe(b: Byte, c: Char, s: Short, i: Int, f: Float)
case class DatastoreTypes(u: Unit, bs: ByteString, ba: Array[Byte], ts: Instant)

case class DefaultInner(i: Int = 1, o: Option[Int] = Some(1), l: List[Int] = List(1, 1))
case class DefaultOuter(
  i: DefaultInner = DefaultInner(2, Some(2), List(2, 2)),
  o: Option[DefaultInner] = Some(DefaultInner(2, Some(2), List(2, 2)))
)

case class LongKey(@key k: Long)
case class StringKey(@key k: String)
case class InstantKey(@key k: Instant)
case class CustomKey(@key("my-project", "com.spotify", "MyKind") k: Long)
case class IntKey(@key k: Int)
case class UriKey(@key k: URI)
case class ByteStringKey(@key k: ByteString)
case class ByteArrayKey(@key k: Array[Byte])
case class DoubleKey(@key @key k: Long)
case class MultiKey(@key k1: Long, @key k2: Long)
case class RecordKey(l: Long, s: String)
case class NestedKey(@key k: RecordKey)

case class EntityIndex(
  default: String,
  @excludeFromIndexes excludedDefault: String,
  @excludeFromIndexes(true) excluded: String,
  @excludeFromIndexes(false) included: String
)

case class DoubleEntityIndex(@excludeFromIndexes(true) @excludeFromIndexes(false) i: Int)

/*
 * Copyright 2019 Spotify AB
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

package magnolify.datastore

import com.google.datastore.v1._
import com.google.datastore.v1.client.DatastoreHelper.makeValue
import com.google.protobuf.{ByteString, NullValue}
import magnolify.shared.{CaseMapper, Converter, EnumType, UnsafeEnum}

import scala.annotation.StaticAnnotation
import scala.collection.compat._
import scala.jdk.CollectionConverters._

import java.time.Instant

sealed trait EntityType[T] extends Converter[T, Entity, Entity.Builder] {
  def apply(v: Entity): T = from(v)
  def apply(v: T): Entity = to(v).build()
}

class key(val project: String = null, val namespace: String = null, val kind: String = null)
    extends StaticAnnotation
    with Serializable
class excludeFromIndexes(val exclude: Boolean = true) extends StaticAnnotation with Serializable

object EntityType {
  def apply[T: EntityField.Record]: EntityType[T] = EntityType(CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: EntityField.Record[T]): EntityType[T] =
    new EntityType[T] {
      private val caseMapper: CaseMapper = cm
      override def from(v: Entity): T = f.fromEntity(v)(caseMapper)
      override def to(v: T): Entity.Builder = f.toEntity(v)(caseMapper)
    }
}

sealed trait KeyField[T] extends Serializable { self =>
  def setKey(b: Key.PathElement.Builder, key: T): Key.PathElement.Builder

  def map[U](f: U => T): KeyField[U] = new KeyField[U] {
    override def setKey(b: Key.PathElement.Builder, key: U): Key.PathElement.Builder =
      self.setKey(b, f(key))
  }
}

object KeyField {
  val longKeyField: KeyField[Long] = new KeyField[Long] {
    override def setKey(b: Key.PathElement.Builder, key: Long): Key.PathElement.Builder =
      b.setId(key)
  }
  val stringKeyField: KeyField[String] = new KeyField[String] {
    override def setKey(b: Key.PathElement.Builder, key: String): Key.PathElement.Builder =
      b.setName(key)
  }
  def notSupported[T]: KeyField[T] = new NotSupported

  def at[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(implicit kf: KeyField[U]): KeyField[T] = kf.map(f)
  }

  class NotSupported[T] extends KeyField[T] {
    override def setKey(b: Key.PathElement.Builder, key: T): Key.PathElement.Builder = ???
    override def map[U](f: U => T): KeyField[U] = new NotSupported
  }
}

trait EntityField[T] extends Serializable {
  def keyField: KeyField[T]
  def from(v: Value)(cm: CaseMapper): T
  def to(v: T)(cm: CaseMapper): Value.Builder
}

object EntityField {
  trait Record[T] extends EntityField[T] {
    def fromEntity(v: Entity)(cm: CaseMapper): T
    def toEntity(v: T)(cm: CaseMapper): Entity.Builder

    override def from(v: Value)(cm: CaseMapper): T = fromEntity(v.getEntityValue)(cm)
    override def to(v: T)(cm: CaseMapper): Value.Builder =
      Value.newBuilder().setEntityValue(toEntity(v)(cm))
  }

  def apply[T](implicit f: EntityField[T]): EntityField[T] = f

  def at[T](f: Value => T)(g: T => Value.Builder)(implicit kf: KeyField[T]): EntityField[T] =
    new EntityField[T] {
      override def keyField: KeyField[T] = kf
      override def from(v: Value)(cm: CaseMapper): T = f(v)
      override def to(v: T)(cm: CaseMapper): Value.Builder = g(v)
    }

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit ef: EntityField[T]): EntityField[U] =
      new EntityField[U] {
        override def keyField: KeyField[U] = ef.keyField.map(g)
        override def from(v: Value)(cm: CaseMapper): U = f(ef.from(v)(cm))
        override def to(v: U)(cm: CaseMapper): Value.Builder = ef.to(g(v))(cm)
      }
  }

  // Entity key supports `Long` and `String` natively
  val efLong = at[Long](_.getIntegerValue)(makeValue)(KeyField.longKeyField)
  val efString = at[String](_.getStringValue)(makeValue)(KeyField.stringKeyField)

  // `Boolean`, `Double` and `Unit` should not be used as keys
  def efBool(implicit kf: KeyField[Boolean]) = at[Boolean](_.getBooleanValue)(makeValue)
  def efDouble(implicit kf: KeyField[Double]) = at[Double](_.getDoubleValue)(makeValue)
  def efUnit(implicit kf: KeyField[Unit]) =
    at[Unit](_ => ())(_ => Value.newBuilder().setNullValue(NullValue.NULL_VALUE))

  // User must provide `KeyField[T]` instances for `ByteString` and `Array[Byte]`
  def efByteString(implicit kf: KeyField[ByteString]) =
    at[ByteString](_.getBlobValue)(makeValue)
  def efByteArray(implicit kf: KeyField[Array[Byte]]) =
    at[Array[Byte]](_.getBlobValue.toByteArray)(v => makeValue(ByteString.copyFrom(v)))

  // Encode `Instant` key as `Long`
  val efTimestamp = at[Instant](TimestampConverter.toInstant)(TimestampConverter.fromInstant)(
    KeyField.at[Instant](_.toEpochMilli)(KeyField.longKeyField)
  )

  def efOption[T](implicit f: EntityField[T]): EntityField[Option[T]] =
    new EntityField[Option[T]] {
      override def keyField: KeyField[Option[T]] = new KeyField[Option[T]] {
        override def setKey(b: Key.PathElement.Builder, key: Option[T]): Key.PathElement.Builder =
          key match {
            case None    => b
            case Some(k) => f.keyField.setKey(b, k)
          }
      }

      override def from(v: Value)(cm: CaseMapper): Option[T] =
        if (v == null) None else Some(f.from(v)(cm))
      override def to(v: Option[T])(cm: CaseMapper): Value.Builder = v match {
        case None    => null
        case Some(x) => f.to(x)(cm)
      }
    }

  def efIterable[T, C[_]](implicit
    f: EntityField[T],
    kf: KeyField[C[T]],
    ti: C[T] => Iterable[T],
    fc: Factory[T, C[T]]
  ): EntityField[C[T]] =
    new EntityField[C[T]] {
      override val keyField: KeyField[C[T]] = kf

      override def from(v: Value)(cm: CaseMapper): C[T] = {
        val b = fc.newBuilder
        if (v != null) b ++= v.getArrayValue.getValuesList.asScala.iterator.map(f.from(_)(cm))
        b.result()
      }

      override def to(v: C[T])(cm: CaseMapper): Value.Builder = {
        val xs = ti(v)
        if (xs.isEmpty) {
          null
        } else {
          Value
            .newBuilder()
            .setArrayValue(
              xs.foldLeft(ArrayValue.newBuilder()) { (b, x) =>
                b.addValues(f.to(x)(cm))
              }.build()
            )
        }
      }
    }

  // unsafe
  val efByte = EntityField.from[Long](_.toByte)(_.toLong)(efLong)
  val efChar = EntityField.from[Long](_.toChar)(_.toLong)(efLong)
  val efShort = EntityField.from[Long](_.toShort)(_.toLong)(efLong)
  val efInt = EntityField.from[Long](_.toInt)(_.toLong)(efLong)
  def efFloat(implicit kf: KeyField[Double]) =
    EntityField.from[Double](_.toFloat)(_.toDouble)(efDouble)

  def efEnum[T](implicit et: EnumType[T]): EntityField[T] =
    EntityField.from[String](et.from)(et.to)(efString)

  def efUnsafeEnum[T](implicit et: EnumType[T]): EntityField[UnsafeEnum[T]] =
    EntityField.from[String](UnsafeEnum.from(_))(UnsafeEnum.to(_))(efString)
}

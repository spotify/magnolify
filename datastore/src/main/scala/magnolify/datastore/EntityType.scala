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
package magnolify.datastore

import com.google.datastore.v1._
import com.google.datastore.v1.client.DatastoreHelper.makeValue
import com.google.protobuf.ByteString
import magnolia._
import magnolify.shared.Converter
import magnolify.shims.FactoryCompat

import scala.collection.JavaConverters._
import scala.language.experimental.macros

sealed trait EntityType[T] extends Converter[T, Entity, Entity.Builder] {
  def apply(v: Entity): T = from(v)
  def apply(v: T): Entity = to(v).build()
}

object EntityType {
  implicit def apply[T](implicit f: EntityField.Record[T]): EntityType[T] = new EntityType[T] {
    override def from(v: Entity): T = f.fromEntity(v)
    override def to(v: T): Entity.Builder = f.toEntity(v)
  }
}

sealed trait EntityField[T] extends Serializable { self =>
  def from(v: Value): T
  def to(v: T): Value.Builder
}

object EntityField {
  trait Record[T] extends EntityField[T] {
    def fromEntity(v: Entity): T
    def toEntity(v: T): Entity.Builder

    override def from(v: Value): T = fromEntity(v.getEntityValue)
    override def to(v: T): Value.Builder = Value.newBuilder().setEntityValue(toEntity(v))
  }

  //////////////////////////////////////////////////

  type Typeclass[T] = EntityField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    override def fromEntity(v: Entity): T =
      caseClass.construct(p => p.typeclass.from(v.getPropertiesOrDefault(p.label, null)))

    override def toEntity(v: T): Entity.Builder =
      caseClass.parameters.foldLeft(Entity.newBuilder()) { (eb, p) =>
        val vb = p.typeclass.to(p.dereference(v))
        if (vb != null) {
          eb.putProperties(p.label, vb.build())
        }
        eb
      }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def gen[T]: Record[T] = macro Magnolia.gen[T]

  //////////////////////////////////////////////////

  def apply[T](implicit f: EntityField[T]): EntityField[T] = f

  def at[T](f: Value => T)(g: T => Value.Builder): EntityField[T] = new EntityField[T] {
    override def from(v: Value): T = f(v)
    override def to(v: T): Value.Builder = g(v)
  }

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit ef: EntityField[T]): EntityField[U] =
      new EntityField[U] {
        override def from(v: Value): U = f(ef.from(v))
        override def to(v: U): Value.Builder = ef.to(g(v))
      }
  }

  //////////////////////////////////////////////////

  implicit val efBool = at[Boolean](_.getBooleanValue)(makeValue)
  implicit val efLong = at[Long](_.getIntegerValue)(makeValue)
  implicit val efDouble = at[Double](_.getDoubleValue)(makeValue)
  implicit val efString = at[String](_.getStringValue)(makeValue)
  implicit val efByteString = at[ByteString](_.getBlobValue)(makeValue)
  implicit val efByteArray =
    at[Array[Byte]](_.getBlobValue.toByteArray)(v => makeValue(ByteString.copyFrom(v)))
  implicit val efTimestamp = at(TimestampConverter.toInstant)(TimestampConverter.fromInstant)

  implicit def efOption[T](implicit f: EntityField[T]): EntityField[Option[T]] =
    new EntityField[Option[T]] {
      override def from(v: Value): Option[T] = if (v == null) None else Some(f.from(v))
      override def to(v: Option[T]): Value.Builder = v match {
        case None    => null
        case Some(x) => f.to(x)
      }
    }

  implicit def efSeq[T, C[T]](
    implicit f: EntityField[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): EntityField[C[T]] =
    new EntityField[C[T]] {
      override def from(v: Value): C[T] =
        if (v == null) {
          fc.newBuilder.result()
        } else {
          fc.build(v.getArrayValue.getValuesList.asScala.iterator.map(f.from))
        }
      override def to(v: C[T]): Value.Builder =
        if (v.isEmpty) {
          null
        } else {
          Value
            .newBuilder()
            .setArrayValue(
              v.foldLeft(ArrayValue.newBuilder()) { (b, x) =>
                  b.addValues(f.to(x))
                }
                .build()
            )
        }
    }
}

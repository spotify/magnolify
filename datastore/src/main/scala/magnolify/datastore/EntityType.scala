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
import com.google.protobuf.{ByteString, NullValue}
import magnolia._
import magnolify.shared.{CaseMapper, Converter}
import magnolify.shims.FactoryCompat
import magnolify.shims.JavaConverters._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

sealed trait EntityType[T] extends Converter[T, Entity, Entity.Builder] {
  def apply(v: Entity): T = from(v)
  def apply(v: T): Entity = to(v).build()
}

object EntityType {
  implicit def apply[T: EntityField.Record]: EntityType[T] = EntityType(CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: EntityField.Record[T]): EntityType[T] =
    new EntityType[T] {
      override protected val caseMapper: CaseMapper = cm
      override def from(v: Entity): T = f.fromEntity(v)(caseMapper)
      override def to(v: T): Entity.Builder = f.toEntity(v)(caseMapper)
    }
}

sealed trait EntityField[T] extends Serializable {
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

  //////////////////////////////////////////////////

  type Typeclass[T] = EntityField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    override def fromEntity(v: Entity)(cm: CaseMapper): T =
      caseClass.construct { p =>
        val f = v.getPropertiesOrDefault(cm.map(p.label), null)
        if (f == null && p.default.isDefined) {
          p.default.get
        } else {
          p.typeclass.from(f)(cm)
        }
      }

    override def toEntity(v: T)(cm: CaseMapper): Entity.Builder =
      caseClass.parameters.foldLeft(Entity.newBuilder()) { (eb, p) =>
        val vb = p.typeclass.to(p.dereference(v))(cm)
        if (vb != null) {
          eb.putProperties(cm.map(p.label), vb.build())
        }
        eb
      }
  }

  @implicitNotFound("Cannot derive EntityField for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def gen[T]: Record[T] = macro Magnolia.gen[T]

  //////////////////////////////////////////////////

  def apply[T](implicit f: EntityField[T]): EntityField[T] = f

  def at[T](f: Value => T)(g: T => Value.Builder): EntityField[T] = new EntityField[T] {
    override def from(v: Value)(cm: CaseMapper): T = f(v)
    override def to(v: T)(cm: CaseMapper): Value.Builder = g(v)
  }

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit ef: EntityField[T]): EntityField[U] =
      new EntityField[U] {
        override def from(v: Value)(cm: CaseMapper): U = f(ef.from(v)(cm))
        override def to(v: U)(cm: CaseMapper): Value.Builder = ef.to(g(v))(cm)
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
  implicit val efUnit =
    at[Unit](_ => ())(_ => Value.newBuilder().setNullValue(NullValue.NULL_VALUE))

  implicit def efOption[T](implicit f: EntityField[T]): EntityField[Option[T]] =
    new EntityField[Option[T]] {
      override def from(v: Value)(cm: CaseMapper): Option[T] =
        if (v == null) None else Some(f.from(v)(cm))
      override def to(v: Option[T])(cm: CaseMapper): Value.Builder = v match {
        case None    => null
        case Some(x) => f.to(x)(cm)
      }
    }

  implicit def efIterable[T, C[_]](implicit
    f: EntityField[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): EntityField[C[T]] =
    new EntityField[C[T]] {
      override def from(v: Value)(cm: CaseMapper): C[T] =
        if (v == null) {
          fc.newBuilder.result()
        } else {
          fc.build(v.getArrayValue.getValuesList.asScala.iterator.map(f.from(_)(cm)))
        }
      override def to(v: C[T])(cm: CaseMapper): Value.Builder =
        if (v.isEmpty) {
          null
        } else {
          Value
            .newBuilder()
            .setArrayValue(
              v.foldLeft(ArrayValue.newBuilder())((b, x) => b.addValues(f.to(x)(cm)))
                .build()
            )
        }
    }
}

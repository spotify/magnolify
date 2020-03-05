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
package magnolify.avro

import java.nio.ByteBuffer
import java.{util => ju}

import magnolia._
import magnolify.shared.Converter
import magnolify.shims.FactoryCompat
import magnolify.shims.JavaConverters._
import org.apache.avro.{JsonProperties, Schema}
import org.apache.avro.generic.{GenericArray, GenericData, GenericRecord, GenericRecordBuilder}

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

sealed trait AvroType[T] extends Converter[T, GenericRecord, GenericRecord] {
  protected val schemaSchema: String
  def schema: Schema = new Schema.Parser().parse(schemaSchema)
  def apply(r: GenericRecord): T = from(r)
  def apply(t: T): GenericRecord = to(t)
}

object AvroType {
  implicit def apply[T](implicit f: AvroField.Record[T]): AvroType[T] = new AvroType[T] {
    override protected val schemaSchema: String = f.schema.toString()
    override def from(v: GenericRecord): T = f.from(v)
    override def to(v: T): GenericRecord = f.to(v)
  }
}

sealed trait AvroField[T] extends Serializable { self =>
  type FromT
  type ToT

  protected val schemaSchema: String
  def schema: Schema = new Schema.Parser().parse(schemaSchema)
  def defaultVal: Any
  def from(v: FromT): T
  def to(v: T): ToT

  def fromAny(v: Any): T = from(v.asInstanceOf[FromT])
}

object AvroField {
  trait Aux[T, From, To] extends AvroField[T] {
    override type FromT = From
    override type ToT = To
  }

  trait Record[T] extends Aux[T, GenericRecord, GenericRecord]

  //////////////////////////////////////////////////

  type Typeclass[T] = AvroField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    override protected val schemaSchema: String = Schema
      .createRecord(
        caseClass.typeName.short,
        null,
        caseClass.typeName.owner,
        false,
        caseClass.parameters.map { p =>
          new Schema.Field(p.label, p.typeclass.schema, null, p.typeclass.defaultVal)
        }.asJava
      )
      .toString()

    override def defaultVal: Any = null

    override def from(v: GenericRecord): T =
      caseClass.construct(p => p.typeclass.fromAny(v.get(p.label)))

    override def to(v: T): GenericRecord =
      caseClass.parameters
        .foldLeft(new GenericRecordBuilder(schema)) { (b, p) =>
          val f = p.typeclass.to(p.dereference(v))
          if (f == null) b else b.set(p.label, f)
        }
        .build()
  }

  @implicitNotFound("Cannot derive AvroField for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def gen[T]: Record[T] = macro Magnolia.gen[T]

  //////////////////////////////////////////////////

  def apply[T](implicit f: AvroField[T]): AvroField[T] = f

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit af: AvroField[T]): AvroField[U] =
      new AvroField[U] {
        override type FromT = af.FromT
        override type ToT = af.ToT
        override protected val schemaSchema: String = af.schema.toString()
        override def defaultVal: Any = af.defaultVal
        override def from(v: FromT): U = f(af.from(v))
        override def to(v: U): ToT = af.to(g(v))
      }
  }

  //////////////////////////////////////////////////

  private def aux[T, From, To](tpe: Schema.Type)(f: From => T)(g: T => To): AvroField[T] =
    new AvroField[T] {
      override type FromT = From
      override type ToT = To
      override protected val schemaSchema: String = Schema.create(tpe).toString()
      override def defaultVal: Any = null
      override def from(v: FromT): T = f(v)
      override def to(v: T): ToT = g(v)
    }

  private def aux2[T, Repr](tpe: Schema.Type)(f: Repr => T)(g: T => Repr): AvroField[T] =
    aux[T, Repr, Repr](tpe)(f)(g)

  private def id[T](tpe: Schema.Type): AvroField[T] = aux[T, T, T](tpe)(identity)(identity)

  implicit val afBoolean = id[Boolean](Schema.Type.BOOLEAN)
  implicit val afInt = id[Int](Schema.Type.INT)
  implicit val afLong = id[Long](Schema.Type.LONG)
  implicit val afFloat = id[Float](Schema.Type.FLOAT)
  implicit val afDouble = id[Double](Schema.Type.DOUBLE)
  implicit val afBytes = aux2[Array[Byte], ByteBuffer](Schema.Type.BYTES)(bb =>
    ju.Arrays.copyOfRange(bb.array(), bb.position(), bb.limit())
  )(ByteBuffer.wrap)
  implicit val afString =
    aux[String, CharSequence, String](Schema.Type.STRING)(_.toString)(identity)

  implicit def afOption[T](implicit f: AvroField[T]): AvroField[Option[T]] =
    new Aux[Option[T], f.FromT, f.ToT] {
      override protected val schemaSchema: String =
        Schema.createUnion(Schema.create(Schema.Type.NULL), f.schema).toString()
      override def defaultVal: Any = JsonProperties.NULL_VALUE
      override def from(v: f.FromT): Option[T] =
        if (v == null) None else Some(f.from(v))
      override def to(v: Option[T]): f.ToT = v match {
        case None    => null.asInstanceOf[f.ToT]
        case Some(x) => f.to(x)
      }
    }

  implicit def afIterable[T, C[_]](
    implicit f: AvroField[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): AvroField[C[T]] =
    new Aux[C[T], ju.List[f.FromT], GenericArray[f.ToT]] {
      override protected val schemaSchema: String = Schema.createArray(f.schema).toString()
      override def defaultVal: Any = ju.Collections.emptyList()
      override def from(v: ju.List[f.FromT]): C[T] =
        if (v == null) fc.newBuilder.result() else fc.build(v.asScala.iterator.map(f.from))
      override def to(v: C[T]): GenericArray[f.ToT] =
        if (v.isEmpty) {
          null
        } else {
          new GenericData.Array[f.ToT](schema, v.iterator.map(f.to(_)).toList.asJava)
        }
    }

  implicit def afMap[T](implicit f: AvroField[T]): AvroField[Map[String, T]] =
    new Aux[Map[String, T], ju.Map[CharSequence, f.FromT], ju.Map[String, f.ToT]] {
      override protected val schemaSchema: String = Schema.createMap(f.schema).toString()
      override def defaultVal: Any = ju.Collections.emptyMap()
      override def from(v: ju.Map[CharSequence, f.FromT]): Map[String, T] =
        if (v == null) {
          Map.empty
        } else {
          v.asScala.iterator.map(kv => (kv._1.toString, f.from(kv._2))).toMap
        }
      override def to(v: Map[String, T]): ju.Map[String, f.ToT] =
        if (v.isEmpty) null else v.iterator.map(kv => (kv._1, f.to(kv._2))).toMap.asJava
    }
}

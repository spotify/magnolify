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
import java.util.stream.Collectors
import java.util.{List => JList, Map => JMap}

import magnolia._
import org.apache.avro.Schema
import org.apache.avro.Schema.Field
import org.apache.avro.generic.{GenericArray, GenericData, GenericRecord}

import scala.collection.JavaConverters._
import scala.language.experimental.macros

trait AvroType[T] extends java.io.Serializable {
  type FromAvroRepr
  type ToAvroRepr

  // Since GenericRecord.get only returns Any
  final def from(r: Any): T = fromAvro(r.asInstanceOf[FromAvroRepr])
  protected def fromAvro(r: FromAvroRepr): T
  def to(t: T): ToAvroRepr
  def schema: Schema
}

object AvroType {
  type Typeclass[T] = AvroType[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): AvroType.Aux2[T, GenericRecord] =
    new AvroType.Aux2[T, GenericRecord] {
      def schema: Schema = {
        val fields = caseClass.parameters.map { param =>
          val fieldSchema = param.typeclass.schema
          new Field(param.label, fieldSchema, "", null)
        }

        Schema.createRecord(caseClass.typeName.short, "", "magnolify.avro", false, fields.asJava)
      }

      override def fromAvro(r: GenericRecord): T = caseClass.construct { p =>
        p.typeclass.from(r.get(p.label))
      }

      override def to(t: T): GenericRecord = {
        val gr = new GenericData.Record(schema)
        caseClass.parameters.foreach { p =>
          gr.put(p.label, p.typeclass.to(p.dereference(t)))
        }
        gr
      }
    }

  // This could maybe be implemented as a union type?
  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???
  implicit def gen[T]: AvroType.Aux2[T, GenericRecord] = macro Magnolia.gen[T]

  trait Passthrough[T] extends Aux2[T, T]
  trait Aux2[T, AvroRepr] extends Aux3[T, AvroRepr, AvroRepr]
  trait Aux3[T, _FromAvroRepr, _ToAvroReprRepr] extends AvroType[T] {
    override type FromAvroRepr = _FromAvroRepr
    override type ToAvroRepr = _ToAvroReprRepr
  }

  object Aux {
    // Primitive/identity fn
    def apply[T](_schema: Schema): AvroType[T] = new AvroType.Passthrough[T] {
      override protected def fromAvro(r: T): T = r
      override def to(t: T): T = t
      override def schema: Schema = _schema
    }
  }

  object Aux2 {
    def apply[T, AvroRepr](
      _schema: Schema,
      _from: AvroRepr => T,
      _to: T => AvroRepr
    ): AvroType[T] = new AvroType.Aux2[T, AvroRepr] {
      override protected def fromAvro(r: AvroRepr): T = _from(r)
      override def to(t: T): AvroRepr = _to(t)
      override def schema: Schema = _schema
    }
  }

  object Aux3 {
    def apply[T, FromAvroRepr, ToAvroRepr](
      _schema: Schema,
      _from: FromAvroRepr => T,
      _to: T => ToAvroRepr
    ): AvroType[T] = new Aux3[T, FromAvroRepr, ToAvroRepr] {
      override protected def fromAvro(r: FromAvroRepr): T = _from(r)
      override def to(t: T): ToAvroRepr = _to(t)
      override def schema: Schema = _schema
    }
  }

  // Implicit instances

  implicit val stringType: AvroType[String] =
    AvroType.Aux3[String, CharSequence, String](
      Schema.create(Schema.Type.STRING),
      _.toString,
      identity
    )
  implicit val booleanType: AvroType[Boolean] =
    AvroType.Aux[Boolean](Schema.create(Schema.Type.BOOLEAN))
  implicit val intType: AvroType[Int] = AvroType.Aux[Int](Schema.create(Schema.Type.INT))
  implicit val longType: AvroType[Long] = AvroType.Aux[Long](Schema.create(Schema.Type.LONG))
  implicit val doubleType: AvroType[Double] =
    AvroType.Aux[Double](Schema.create(Schema.Type.DOUBLE))
  implicit val floatType: AvroType[Float] = AvroType.Aux[Float](Schema.create(Schema.Type.FLOAT))
  implicit val bytesType: AvroType[Array[Byte]] = AvroType
    .Aux2[Array[Byte], ByteBuffer](Schema.create(Schema.Type.BYTES), _.array, ByteBuffer.wrap)

  implicit def repeatedType[T: AvroType]: AvroType[List[T]] = {
    val tc = implicitly[AvroType[T]]
    val schema = Schema.createArray(tc.schema)

    AvroType.Aux3[List[T], JList[tc.FromAvroRepr], GenericArray[tc.ToAvroRepr]](
      schema,
      (f: JList[tc.FromAvroRepr]) => f.asScala.map(tc.fromAvro).toList,
      (t: List[T]) => new GenericData.Array(schema, t.map(tc.to).asJava)
    )
  }

  implicit def nullableType[T: AvroType]: AvroType[Option[T]] = {
    val tc = implicitly[AvroType[T]]

    def mapToNull(t: Option[T]): tc.ToAvroRepr = t.map(tc.to) match {
      case Some(to) => to
      case None     => null.asInstanceOf[tc.ToAvroRepr]
    }

    AvroType.Aux3[Option[T], tc.FromAvroRepr, tc.ToAvroRepr](
      Schema.createUnion(Schema.create(Schema.Type.NULL), tc.schema),
      (f: tc.FromAvroRepr) => Option(f).map(tc.fromAvro),
      mapToNull
    )
  }

  implicit def mapType[T: AvroType]: AvroType[Map[String, T]] = {
    val tc = implicitly[AvroType[T]]

    AvroType.Aux3[Map[String, T], JMap[String, tc.FromAvroRepr], JMap[String, tc.ToAvroRepr]](
      Schema.createMap(tc.schema),
      (f: JMap[String, tc.FromAvroRepr]) => f.asScala.toMap.map { case (k, v) => k -> tc.from(v) },
      (t: Map[String, T]) =>
        t.asJava.entrySet.stream.collect(
          Collectors.toMap(
            (kv: JMap.Entry[String, T]) => kv.getKey,
            (kv: JMap.Entry[String, T]) => tc.to(kv.getValue)
          )
        )
    )
  }
}

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
import Converter._
import magnolify.shims.FactoryCompat
import magnolify.shims.JavaConverters._
import org.apache.avro.{JsonProperties, Schema}
import org.apache.avro.generic.{GenericArray, GenericData, GenericRecord, GenericRecordBuilder}

import scala.annotation.{implicitNotFound, StaticAnnotation}
import scala.language.experimental.macros
import scala.language.implicitConversions

class doc(doc: String) extends StaticAnnotation with Serializable {
  override def toString: String = doc
}

sealed abstract class AvroType[T](g: CaseMapper = identity) extends Converter[T, GenericRecord, GenericRecord] {
  protected val schemaString: String
  def schema: Schema = new Schema.Parser().parse(schemaString)
  def apply(r: GenericRecord): T = from(r)
  def apply(t: T): GenericRecord = to(t)
}

object AvroType {
  implicit def apply[T](implicit f: AvroField.Record[T]): AvroType[T] = new AvroType[T]() {
    override protected val schemaString: String = f.schema(identity).toString()
    override def from(v: GenericRecord): T = f.from(v)(identity)
    override def to(v: T): GenericRecord = f.to(v)(identity)
  }

  implicit def apply[T](g: CaseMapper)(implicit f: AvroField.Record[T]): AvroType[T] = new AvroType[T](g) {
    override protected val schemaString: String = f.schema(g).toString()
    override def from(v: GenericRecord): T = f.from(v)(g)
    override def to(v: T): GenericRecord = f.to(v)(g)
  }

}

sealed trait AvroField[T] extends Serializable {
  type FromT
  type ToT

  protected def schemaString(g: CaseMapper): String
  def schema(g: CaseMapper): Schema = new Schema.Parser().parse(schemaString(g))
  def defaultVal: Any
  def from(v: FromT)(g: CaseMapper): T
  def to(v: T)(g: CaseMapper): ToT

  def fromAny(v: Any)(g: CaseMapper): T = from(v.asInstanceOf[FromT])(g)
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
    override protected def schemaString(g: CaseMapper): String = Schema
      .createRecord(
        caseClass.typeName.short,
        getDoc(caseClass.annotations, caseClass.typeName.full),
        caseClass.typeName.owner,
        false,
        caseClass.parameters.map { p =>
          new Schema.Field(
            g(p.label),
            p.typeclass.schema(g),
            getDoc(p.annotations, s"${caseClass.typeName.full}#${g(p.label)}"),
            p.typeclass.defaultVal
          )
        }.asJava
      )
      .toString()

    override def defaultVal: Any = null

    override def from(v: GenericRecord)(g: CaseMapper): T =
      caseClass.construct(p => p.typeclass.fromAny(v.get(g(p.label)))(g))

    override def to(v: T)(g: CaseMapper): GenericRecord =
      caseClass.parameters
        .foldLeft(new GenericRecordBuilder(schema(g))) { (b, p) =>
          val f = p.typeclass.to(p.dereference(v))(g)
          if (f == null) b else b.set(g(p.label), f)
        }
        .build()

    private def getDoc(annotations: Seq[Any], name: String): String = {
      val docs = annotations.collect { case d: doc => d.toString }
      require(docs.size <= 1, s"More than one @doc annotation: $name")
      docs.headOption.orNull
    }
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
        override protected def schemaString(c: CaseMapper): String = af.schema(c).toString()
        override def defaultVal: Any = af.defaultVal
        override def from(v: FromT)(c: CaseMapper): U = f(af.from(v)(c))
        override def to(v: U)(c: CaseMapper): ToT = af.to(g(v))(c)
      }
  }

  //////////////////////////////////////////////////

  private def aux[T, From, To](tpe: Schema.Type)(f: From => T)(g: T => To): AvroField[T] =
    new AvroField[T] {
      override type FromT = From
      override type ToT = To
      override protected def schemaString(c: CaseMapper): String = Schema.create(tpe).toString()
      override def defaultVal: Any = null
      override def from(v: FromT)(c: CaseMapper): T = f(v)
      override def to(v: T)(c: CaseMapper): ToT = g(v)
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
      override protected def schemaString(c: CaseMapper): String =
        Schema.createUnion(Schema.create(Schema.Type.NULL), f.schema(c)).toString()
      override def defaultVal: Any = JsonProperties.NULL_VALUE
      override def from(v: f.FromT)(c: CaseMapper): Option[T] =
        if (v == null) None else Some(f.from(v)(c))
      override def to(v: Option[T])(c: CaseMapper): f.ToT = v match {
        case None    => null.asInstanceOf[f.ToT]
        case Some(x) => f.to(x)(c)
      }
    }

  implicit def afIterable[T, C[_]](implicit
    f: AvroField[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): AvroField[C[T]] =
    new Aux[C[T], ju.List[f.FromT], GenericArray[f.ToT]] {
      override protected def schemaString(c: CaseMapper): String = Schema.createArray(f.schema(c)).toString()
      override def defaultVal: Any = ju.Collections.emptyList()
      override def from(v: ju.List[f.FromT])(c: CaseMapper): C[T] =
        if (v == null) fc.newBuilder.result() else fc.build(v.asScala.iterator.map(p => f.from(p)(c)))
      override def to(v: C[T])(c: CaseMapper): GenericArray[f.ToT] =
        if (v.isEmpty) {
          null
        } else {
          new GenericData.Array[f.ToT](schema(c), v.iterator.map(f.to(_)(c)).toList.asJava)
        }
    }

  implicit def afMap[T](implicit f: AvroField[T]): AvroField[Map[String, T]] =
    new Aux[Map[String, T], ju.Map[CharSequence, f.FromT], ju.Map[String, f.ToT]] {
      override protected def schemaString(c: CaseMapper): String = Schema.createMap(f.schema(c)).toString()
      override def defaultVal: Any = ju.Collections.emptyMap()
      override def from(v: ju.Map[CharSequence, f.FromT])(c: CaseMapper): Map[String, T] =
        if (v == null) {
          Map.empty
        } else {
          v.asScala.iterator.map(kv => (kv._1.toString, f.from(kv._2)(c))).toMap
        }
      override def to(v: Map[String, T])(c: CaseMapper): ju.Map[String, f.ToT] =
        if (v.isEmpty) null else v.iterator.map(kv => (kv._1, f.to(kv._2)(c))).toMap.asJava
    }
}

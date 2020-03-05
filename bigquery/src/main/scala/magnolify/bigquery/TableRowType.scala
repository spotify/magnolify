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
package magnolify.bigquery

import java.{util => ju}

import com.google.api.client.json.GenericJson
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.bigquery.model.{TableFieldSchema, TableRow, TableSchema}
import com.google.common.io.BaseEncoding
import magnolia._
import magnolify.shared.Converter
import magnolify.shims.FactoryCompat
import magnolify.shims.JavaConverters._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.reflect.ClassTag

sealed trait TableRowType[T] extends Converter[T, TableRow, TableRow] {
  protected val schemaString: String
  def schema: TableSchema = Schemas.fromJson[TableSchema](schemaString)
  def apply(v: TableRow): T = from(v)
  def apply(v: T): TableRow = to(v)
}

object TableRowType {
  implicit def apply[T](implicit f: TableRowField.Record[T]): TableRowType[T] =
    new TableRowType[T] {
      override protected val schemaString: String =
        Schemas.toJson(new TableSchema().setFields(f.fieldSchema.getFields))

      override def from(v: TableRow): T = f.from(v)
      override def to(v: T): TableRow = f.to(v)
    }
}

sealed trait TableRowField[T] extends Serializable { self =>
  type FromT
  type ToT

  protected val schemaString: String
  def fieldSchema: TableFieldSchema = Schemas.fromJson[TableFieldSchema](schemaString)
  def from(v: FromT): T
  def to(v: T): ToT

  def fromAny(v: Any): T = from(v.asInstanceOf[FromT])
}

object TableRowField {
  trait Aux[T, From, To] extends TableRowField[T] {
    override type FromT = From
    override type ToT = To
  }

  trait Generic[T] extends Aux[T, Any, Any]
  trait Record[T] extends Aux[T, java.util.Map[String, AnyRef], TableRow]

  //////////////////////////////////////////////////

  type Typeclass[T] = TableRowField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    override protected val schemaString: String =
      Schemas.toJson(
        new TableFieldSchema()
          .setType("STRUCT")
          .setMode("REQUIRED")
          .setFields(caseClass.parameters.map(p => p.typeclass.fieldSchema.setName(p.label)).asJava)
      )

    override def from(v: java.util.Map[String, AnyRef]): T =
      caseClass.construct(p => p.typeclass.fromAny(v.get(p.label)))

    override def to(v: T): TableRow =
      caseClass.parameters.foldLeft(new TableRow) { (tr, p) =>
        val f = p.typeclass.to(p.dereference(v))
        if (f != null) {
          tr.put(p.label, f)
        }
        tr
      }
  }

  @implicitNotFound("Cannot derive TableRowField for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def gen[T]: Record[T] = macro Magnolia.gen[T]

  //////////////////////////////////////////////////

  def apply[T](implicit f: TableRowField[T]): TableRowField[T] = f

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit trf: TableRowField[T]): TableRowField[U] =
      new TableRowField[U] {
        override type FromT = trf.FromT
        override type ToT = trf.ToT
        override protected val schemaString: String = Schemas.toJson(trf.fieldSchema)
        override def from(v: FromT): U = f(trf.from(v))
        override def to(v: U): ToT = trf.to(g(v))
      }
  }

  //////////////////////////////////////////////////

  private def at[T](tpe: String)(f: Any => T)(g: T => Any): TableRowField[T] = new Generic[T] {
    override protected val schemaString: String =
      Schemas.toJson(new TableFieldSchema().setType(tpe).setMode("REQUIRED"))
    override def from(v: Any): T = f(v)
    override def to(v: T): Any = g(v)
  }

  implicit val trfBool = at[Boolean]("BOOL")(_.toString.toBoolean)(identity)
  implicit val trfLong = at[Long]("INT64")(_.toString.toLong)(identity)
  implicit val trfDouble = at[Double]("FLOAT64")(_.toString.toDouble)(identity)
  implicit val trfString = at[String]("STRING")(_.toString)(identity)
  implicit val trfNumeric =
    at[BigDecimal]("NUMERIC")(NumericConverter.toBigDecimal)(NumericConverter.fromBigDecimal)

  implicit val trfByteArray =
    at[Array[Byte]]("BYTES")(x => BaseEncoding.base64().decode(x.toString))(x =>
      BaseEncoding.base64().encode(x)
    )

  import TimestampConverter._
  implicit val trfInstant = at("TIMESTAMP")(toInstant)(fromInstant)
  implicit val trfDate = at("DATE")(toLocalDate)(fromLocalDate)
  implicit val trfTime = at("TIME")(toLocalTime)(fromLocalTime)
  implicit val trfDateTime = at("DATETIME")(toLocalDateTime)(fromLocalDateTime)

  implicit def trfOption[T](implicit f: TableRowField[T]): TableRowField[Option[T]] =
    new Aux[Option[T], f.FromT, f.ToT] {
      override protected val schemaString: String =
        Schemas.toJson(f.fieldSchema.setMode("NULLABLE"))
      override def from(v: f.FromT): Option[T] =
        if (v == null) None else Some(f.from(v))
      override def to(v: Option[T]): f.ToT = v match {
        case None    => null.asInstanceOf[f.ToT]
        case Some(x) => f.to(x)
      }
    }

  implicit def trfIterable[T, C[_]](
    implicit f: TableRowField[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): TableRowField[C[T]] =
    new Aux[C[T], ju.List[f.FromT], ju.List[f.ToT]] {
      override protected val schemaString: String =
        Schemas.toJson(f.fieldSchema.setMode("REPEATED"))
      override def from(v: ju.List[f.FromT]): C[T] =
        if (v == null) {
          fc.newBuilder.result()
        } else {
          fc.build(v.asScala.iterator.map(f.from))
        }
      override def to(v: C[T]): ju.List[f.ToT] =
        if (v.isEmpty) null else v.iterator.map(f.to(_)).toList.asJava
    }
}

private object Schemas {
  private val jsonFactory = JacksonFactory.getDefaultInstance
  def toJson(schema: TableSchema) = jsonFactory.toString(schema)
  def toJson(schema: TableFieldSchema) = jsonFactory.toString(schema)
  def fromJson[T <: GenericJson](schemaString: String)(implicit ct: ClassTag[T]): T =
    jsonFactory.fromString(schemaString, ct.runtimeClass).asInstanceOf[T]
}

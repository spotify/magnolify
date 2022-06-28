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

package magnolify.bigquery

import java.{util => ju}

import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.bigquery.model.{TableFieldSchema, TableRow, TableSchema}
import com.google.common.io.BaseEncoding
import magnolia1._
import magnolify.shared.{CaseMapper, Converter}
import magnolify.shims.FactoryCompat
import magnolify.shims.JavaConverters._

import scala.annotation.{implicitNotFound, StaticAnnotation}
import scala.language.experimental.macros

class description(description: String) extends StaticAnnotation with Serializable {
  override def toString: String = description
}

sealed trait TableRowType[T] extends Converter[T, TableRow, TableRow] {
  val schema: TableSchema
  val description: String
  def apply(v: TableRow): T = from(v)
  def apply(v: T): TableRow = to(v)
}

object TableRowType {
  implicit def apply[T: TableRowField.Record]: TableRowType[T] = TableRowType(CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: TableRowField.Record[T]): TableRowType[T] = {
    f.fieldSchema(cm) // fail fast on bad annotations
    new TableRowType[T] {
      private val caseMapper: CaseMapper = cm
      @transient override lazy val schema: TableSchema =
        new TableSchema().setFields(f.fieldSchema(caseMapper).getFields)
      override val description: String = f.fieldSchema(caseMapper).getDescription
      override def from(v: TableRow): T = f.from(v)(caseMapper)
      override def to(v: T): TableRow = f.to(v)(caseMapper)
    }
  }
}

sealed trait TableRowField[T] extends Serializable {
  type FromT
  type ToT

  protected def schemaString(cm: CaseMapper): String
  def fieldSchema(cm: CaseMapper): TableFieldSchema =
    Schemas.fromJson(schemaString(cm))
  def from(v: FromT)(cm: CaseMapper): T
  def to(v: T)(cm: CaseMapper): ToT

  def fromAny(v: Any)(cm: CaseMapper): T = from(v.asInstanceOf[FromT])(cm)
}

object TableRowField {
  sealed trait Aux[T, From, To] extends TableRowField[T] {
    override type FromT = From
    override type ToT = To
  }

  sealed trait Generic[T] extends Aux[T, Any, Any]
  sealed trait Record[T] extends Aux[T, java.util.Map[String, AnyRef], TableRow]

  // ////////////////////////////////////////////////

  type Typeclass[T] = TableRowField[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    override protected def schemaString(cm: CaseMapper): String =
      Schemas.toJson(
        new TableFieldSchema()
          .setType("STRUCT")
          .setMode("REQUIRED")
          .setDescription(getDescription(caseClass.annotations, caseClass.typeName.full))
          .setFields(caseClass.parameters.map { p =>
            p.typeclass
              .fieldSchema(cm)
              .setName(cm.map(p.label))
              .setDescription(
                getDescription(p.annotations, s"${caseClass.typeName.full}#${p.label}")
              )
          }.asJava)
      )

    override def from(v: java.util.Map[String, AnyRef])(cm: CaseMapper): T =
      caseClass.construct { p =>
        val f = v.get(cm.map(p.label))
        if (f == null && p.default.isDefined) {
          p.default.get
        } else {
          p.typeclass.fromAny(f)(cm)
        }
      }

    override def to(v: T)(cm: CaseMapper): TableRow =
      caseClass.parameters.foldLeft(new TableRow) { (tr, p) =>
        val f = p.typeclass.to(p.dereference(v))(cm)
        if (f != null) {
          tr.put(cm.map(p.label), f)
        }
        tr
      }

    private def getDescription(annotations: Seq[Any], name: String): String = {
      val descs = annotations.collect { case d: description => d.toString }
      require(descs.size <= 1, s"More than one @description annotation: $name")
      descs.headOption.orNull
    }
  }

  @implicitNotFound("Cannot derive TableRowField for sealed trait")
  private sealed trait Dispatchable[T]
  def split[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def gen[T]: Record[T] = macro Magnolia.gen[T]

  // ////////////////////////////////////////////////

  def apply[T](implicit f: TableRowField[T]): TableRowField[T] = f

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit trf: TableRowField[T]): TableRowField[U] =
      new TableRowField[U] {
        override type FromT = trf.FromT
        override type ToT = trf.ToT
        override protected def schemaString(cm: CaseMapper): String =
          Schemas.toJson(trf.fieldSchema(cm))
        override def from(v: FromT)(cm: CaseMapper): U = f(trf.from(v)(cm))
        override def to(v: U)(cm: CaseMapper): ToT = trf.to(g(v))(cm)
      }
  }

  // ////////////////////////////////////////////////

  private def at[T](tpe: String)(f: Any => T)(g: T => Any): TableRowField[T] = new Generic[T] {
    private val _schemaString =
      Schemas.toJson(new TableFieldSchema().setType(tpe).setMode("REQUIRED"))
    override protected def schemaString(cm: CaseMapper): String = _schemaString
    override def from(v: Any)(cm: CaseMapper): T = f(v)
    override def to(v: T)(cm: CaseMapper): Any = g(v)
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
      override protected def schemaString(cm: CaseMapper): String =
        Schemas.toJson(f.fieldSchema(cm).setMode("NULLABLE"))
      override def from(v: f.FromT)(cm: CaseMapper): Option[T] =
        if (v == null) None else Some(f.from(v)(cm))
      override def to(v: Option[T])(cm: CaseMapper): f.ToT = v match {
        case None    => null.asInstanceOf[f.ToT]
        case Some(x) => f.to(x)(cm)
      }
    }

  implicit def trfIterable[T, C[_]](implicit
    f: TableRowField[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): TableRowField[C[T]] =
    new Aux[C[T], ju.List[f.FromT], ju.List[f.ToT]] {
      override protected def schemaString(cm: CaseMapper): String =
        Schemas.toJson(f.fieldSchema(cm).setMode("REPEATED"))
      override def from(v: ju.List[f.FromT])(cm: CaseMapper): C[T] =
        if (v == null) {
          fc.newBuilder.result()
        } else {
          fc.build(v.asScala.iterator.map(f.from(_)(cm)))
        }
      override def to(v: C[T])(cm: CaseMapper): ju.List[f.ToT] =
        if (v.isEmpty) null else v.iterator.map(f.to(_)(cm)).toList.asJava
    }
}

private object Schemas {
  private val jsonFactory = GsonFactory.getDefaultInstance
  def toJson(schema: TableFieldSchema) = jsonFactory.toString(schema)
  def fromJson(schemaString: String): TableFieldSchema =
    jsonFactory.fromString(schemaString, classOf[TableFieldSchema])
}

private object NumericConverter {
  // https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types#numeric-type
  private val MaxPrecision = 38
  private val MaxScale = 9

  def toBigDecimal(v: Any): BigDecimal = BigDecimal(v.toString)
  def fromBigDecimal(v: BigDecimal): Any = {
    require(
      v.precision <= MaxPrecision,
      s"Cannot encode BigDecimal $v: precision ${v.precision} > $MaxPrecision"
    )
    require(v.scale <= MaxScale, s"Cannot encode BigDecimal $v: scale ${v.scale} > $MaxScale")
    v.toString()
  }
}

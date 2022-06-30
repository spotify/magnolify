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

import java.util.UUID

import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.bigquery.model.{TableFieldSchema, TableRow, TableSchema}
import com.google.common.io.BaseEncoding
import magnolify.shared.{CaseMapper, Converter, EnumType, UnsafeEnum}
import scala.annotation.StaticAnnotation
import scala.collection.concurrent
import scala.language.experimental.macros

import scala.collection.compat._
import scala.jdk.CollectionConverters._

class description(description: String) extends StaticAnnotation with Serializable {
  override def toString: String = description
}

sealed trait TableRowType[T] extends Converter[T, TableRow, TableRow] {
  def schema: TableSchema
  def description: String
  def apply(v: TableRow): T = from(v)
  def apply(v: T): TableRow = to(v)
}

object TableRowType {
  def apply[T: TableRowField.Record]: TableRowType[T] = TableRowType(CaseMapper.identity)

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

  @transient private lazy val schemaCache: concurrent.Map[UUID, TableFieldSchema] =
    concurrent.TrieMap.empty

  protected def buildSchema(cm: CaseMapper): TableFieldSchema
  def fieldSchema(cm: CaseMapper): TableFieldSchema =
    schemaCache.getOrElseUpdate(cm.uuid, buildSchema(cm))

  def from(v: FromT)(cm: CaseMapper): T
  def to(v: T)(cm: CaseMapper): ToT

  def fromAny(v: Any)(cm: CaseMapper): T = from(v.asInstanceOf[FromT])(cm)
}

object TableRowField {
  trait Aux[T, From, To] extends TableRowField[T] {
    override type FromT = From
    override type ToT = To
  }

  trait Generic[T] extends Aux[T, Any, Any]
  trait Record[T] extends Aux[T, java.util.Map[String, AnyRef], TableRow]

  def apply[T](implicit f: TableRowField[T]): TableRowField[T] = f

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit trf: TableRowField[T]): TableRowField[U] =
      new TableRowField[U] {
        override type FromT = trf.FromT
        override type ToT = trf.ToT

        override protected def buildSchema(cm: CaseMapper): TableFieldSchema =
          trf.fieldSchema(cm)
        override def from(v: FromT)(cm: CaseMapper): U = f(trf.from(v)(cm))
        override def to(v: U)(cm: CaseMapper): ToT = trf.to(g(v))(cm)
      }
  }

  // ////////////////////////////////////////////////

  private def at[T](tpe: String)(f: Any => T)(g: T => Any): TableRowField[T] = new Generic[T] {
    override protected def buildSchema(cm: CaseMapper): TableFieldSchema =
      new TableFieldSchema().setType(tpe).setMode("REQUIRED")
    override def from(v: Any)(cm: CaseMapper): T = f(v)
    override def to(v: T)(cm: CaseMapper): Any = g(v)
  }

  val trfBool = at[Boolean]("BOOL")(_.toString.toBoolean)(identity)
  val trfLong = at[Long]("INT64")(_.toString.toLong)(identity)
  val trfDouble = at[Double]("FLOAT64")(_.toString.toDouble)(identity)
  val trfString = at[String]("STRING")(_.toString)(identity)
  val trfNumeric =
    at[BigDecimal]("NUMERIC")(NumericConverter.toBigDecimal)(NumericConverter.fromBigDecimal)

  val trfByteArray =
    at[Array[Byte]]("BYTES")(x => BaseEncoding.base64().decode(x.toString))(x =>
      BaseEncoding.base64().encode(x)
    )

  import TimestampConverter._
  val trfInstant = at("TIMESTAMP")(toInstant)(fromInstant)
  val trfDate = at("DATE")(toLocalDate)(fromLocalDate)
  val trfTime = at("TIME")(toLocalTime)(fromLocalTime)
  val trfDateTime = at("DATETIME")(toLocalDateTime)(fromLocalDateTime)

  def trfOption[T](implicit f: TableRowField[T]): TableRowField[Option[T]] =
    new Aux[Option[T], f.FromT, f.ToT] {
      override protected def buildSchema(cm: CaseMapper): TableFieldSchema =
        f.fieldSchema(cm).clone().setMode("NULLABLE")
      override def from(v: f.FromT)(cm: CaseMapper): Option[T] =
        if (v == null) None else Some(f.from(v)(cm))
      override def to(v: Option[T])(cm: CaseMapper): f.ToT = v match {
        case None    => null.asInstanceOf[f.ToT]
        case Some(x) => f.to(x)(cm)
      }
    }

  def trfIterable[T, C[_]](implicit
    f: TableRowField[T],
    ti: C[T] => Iterable[T],
    fc: Factory[T, C[T]]
  ): TableRowField[C[T]] =
    new Aux[C[T], java.util.List[f.FromT], java.util.List[f.ToT]] {
      override protected def buildSchema(cm: CaseMapper): TableFieldSchema =
        f.fieldSchema(cm).clone().setMode("REPEATED")
      override def from(v: java.util.List[f.FromT])(cm: CaseMapper): C[T] = {
        if (v == null) {
          fc.newBuilder.result()
        } else {
          val b = fc.newBuilder
          b ++= v.asScala.iterator.map(f.from(_)(cm))
          b.result()
        }
      }

      override def to(v: C[T])(cm: CaseMapper): java.util.List[f.ToT] = {
        val xs = ti(v)
        if (xs.isEmpty) null else xs.iterator.map(f.to(_)(cm)).toList.asJava
      }
    }

  // unsafe
  val trfByte: TableRowField[Byte] =
    TableRowField.from[Long](_.toByte)(_.toLong)(trfLong)
  val trfChar: TableRowField[Char] =
    TableRowField.from[Long](_.toChar)(_.toLong)(trfLong)
  val trfShort: TableRowField[Short] =
    TableRowField.from[Long](_.toShort)(_.toLong)(trfLong)
  val trfInt: TableRowField[Int] =
    TableRowField.from[Long](_.toInt)(_.toLong)(trfLong)
  val trfFloat: TableRowField[Float] =
    TableRowField.from[Double](_.toFloat)(_.toDouble)(trfDouble)

  def trfEnum[T](implicit et: EnumType[T]): TableRowField[T] =
    TableRowField.from[String](et.from)(et.to)(trfString)

  def trfUnsafeEnum[T](implicit et: EnumType[T]): TableRowField[UnsafeEnum[T]] =
    TableRowField.from[String](UnsafeEnum.from(_))(UnsafeEnum.to(_))(trfString)
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

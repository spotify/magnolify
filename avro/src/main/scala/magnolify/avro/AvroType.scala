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

package magnolify.avro

import java.util.UUID
import magnolify.shared._
import org.apache.avro.JsonProperties
import org.apache.avro.generic.GenericData.EnumSymbol

import java.nio.ByteBuffer
import java.time.LocalDate
import org.apache.avro.generic._
import org.apache.avro.{LogicalType, LogicalTypes, Schema}

import scala.annotation.StaticAnnotation
import scala.collection.concurrent
import scala.collection.compat._
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._

class doc(doc: String) extends StaticAnnotation with Serializable {
  override def toString: String = doc
}

trait AvroType[T] extends Converter[T, GenericRecord, GenericRecord] {
  def schema: Schema
  def apply(r: GenericRecord): T = from(r)
  def apply(t: T): GenericRecord = to(t)
}

object AvroType {
  def apply[T: AvroField.Record]: AvroType[T] = AvroType(CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: AvroField.Record[T]): AvroType[T] = {
    f.schema(cm) // fail fast on bad annotations
    new AvroType[T] {
      private val caseMapper: CaseMapper = cm
      @transient override lazy val schema: Schema = f.schema(caseMapper)
      override def from(v: GenericRecord): T = f.from(v)(caseMapper)
      override def to(v: T): GenericRecord = f.to(v)(caseMapper)
    }
  }
}

trait AvroField[T] extends Serializable { self =>
  type FromT
  type ToT

  @transient private lazy val schemaCache: concurrent.Map[UUID, Schema] =
    concurrent.TrieMap.empty

  protected def buildSchema(cm: CaseMapper): Schema
  def schema(cm: CaseMapper): Schema =
    schemaCache.getOrElseUpdate(cm.uuid, buildSchema(cm))

  // Convert default `T` to Avro schema default value
  def makeDefault(d: T)(cm: CaseMapper): Any = to(d)(cm)

  // Fallback Avro schema default value
  def fallbackDefault: Any = null

  def from(v: FromT)(cm: CaseMapper): T
  def to(v: T)(cm: CaseMapper): ToT

  def fromAny(v: Any)(cm: CaseMapper): T = from(v.asInstanceOf[FromT])(cm)
}

object AvroField {
  trait Aux[T, From, To] extends AvroField[T] {
    override type FromT = From
    override type ToT = To
  }

  trait Record[T] extends Aux[T, GenericRecord, GenericRecord]

  def apply[T](implicit f: AvroField[T]): AvroField[T] = f

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit af: AvroField[T]): AvroField[U] =
      new Aux[U, af.FromT, af.ToT] {
        override protected def buildSchema(cm: CaseMapper): Schema = af.schema(cm)
        override def makeDefault(d: U)(cm: CaseMapper): Any = af.makeDefault(g(d))(cm)
        override def from(v: FromT)(cm: CaseMapper): U = f(af.from(v)(cm))
        override def to(v: U)(cm: CaseMapper): ToT = af.to(g(v))(cm)
      }
  }

  def aux[T, From, To](tpe: Schema.Type)(f: From => T)(g: T => To): AvroField[T] =
    new Aux[T, From, To] {
      override protected def buildSchema(cm: CaseMapper): Schema = Schema.create(tpe)
      override def from(v: FromT)(cm: CaseMapper): T = f(v)
      override def to(v: T)(cm: CaseMapper): ToT = g(v)
    }

  def aux2[T, Repr](tpe: Schema.Type)(f: Repr => T)(g: T => Repr): AvroField[T] =
    aux[T, Repr, Repr](tpe)(f)(g)

  def id[T](tpe: Schema.Type): AvroField[T] = aux[T, T, T](tpe)(identity)(identity)

  def logicalType[T](lt: => LogicalType): LogicalTypeWord[T] = new LogicalTypeWord[T](lt)

  class LogicalTypeWord[T](lt: => LogicalType) extends Serializable {
    def apply[U](f: T => U)(g: U => T)(implicit af: AvroField[T]): AvroField[U] =
      new Aux[U, af.FromT, af.ToT] {
        override protected def buildSchema(cm: CaseMapper): Schema = {
          // `LogicalType#addToSchema` mutates `Schema`, make a copy first
          val schema = new Schema.Parser().parse(af.schema(cm).toString)
          lt.addToSchema(schema)
        }

        override def makeDefault(d: U)(cm: CaseMapper): Any = af.makeDefault(g(d))(cm)
        override def from(v: FromT)(cm: CaseMapper): U = f(af.from(v)(cm))
        override def to(v: U)(cm: CaseMapper): ToT = af.to(g(v))(cm)
      }
  }

  private[avro] def getDoc(annotations: Seq[Any], name: String): String = {
    val docs = annotations.collect { case d: doc => d.toString }
    require(docs.size <= 1, s"More than one @doc annotation: $name")
    docs.headOption.orNull
  }

  // https://avro.apache.org/docs/current/spec.html#Logical+Types
  // Precision and scale are not encoded in the `BigDecimal` type and must be specified
  def bigDecimal(precision: Int, scale: Int = 0): AvroField[BigDecimal] =
    logicalType[Array[Byte]](LogicalTypes.decimal(precision, scale))(
      Decimal.fromBytes(_, precision, scale)
    )(Decimal.toBytes(_, precision, scale))(afBytes)

  def fixed[T: ClassTag](
    size: Int
  )(f: Array[Byte] => T)(g: T => Array[Byte]) /* (implicit an: AnnotationType[T]) */: AvroField[T] =
    new Aux[T, GenericFixed, GenericFixed] {
      override protected def buildSchema(cm: CaseMapper): Schema = {
        val n = ReflectionUtils.name[T]
        val ns = ReflectionUtils.namespace[T]
        Schema.createFixed(n, /*getDoc(an.annotations, n)*/ "", ns, size)
      }

      override def from(v: GenericFixed)(cm: CaseMapper): T = f(v.bytes())
      override def to(v: T)(cm: CaseMapper): GenericFixed = new GenericData.Fixed(schema(cm), g(v))
    }

  val afBoolean: AvroField[Boolean] = AvroField.id[Boolean](Schema.Type.BOOLEAN)
  val afInt: AvroField[Int] = AvroField.id[Int](Schema.Type.INT)
  val afLong: AvroField[Long] = AvroField.id[Long](Schema.Type.LONG)
  val afFloat: AvroField[Float] = AvroField.id[Float](Schema.Type.FLOAT)
  val afDouble: AvroField[Double] = AvroField.id[Double](Schema.Type.DOUBLE)
  val afString: AvroField[String] =
    AvroField.aux[String, CharSequence, String](Schema.Type.STRING)(_.toString)(identity)
  val afUnit: AvroField[Unit] =
    AvroField.aux2[Unit, JsonProperties.Null](Schema.Type.NULL)(_ => ())(_ =>
      JsonProperties.NULL_VALUE
    )

  val afBytes: AvroField[Array[Byte]] = new AvroField[Array[Byte]] {
    override type FromT = ByteBuffer
    override type ToT = ByteBuffer

    override protected def buildSchema(cm: CaseMapper): Schema =
      Schema.create(Schema.Type.BYTES)
    // `JacksonUtils.toJson` expects `Array[Byte]` for `BYTES` defaults
    override def makeDefault(d: Array[Byte])(cm: CaseMapper): Array[Byte] = d
    override def from(v: ByteBuffer)(cm: CaseMapper): Array[Byte] =
      java.util.Arrays.copyOfRange(v.array(), v.position(), v.limit())
    override def to(v: Array[Byte])(cm: CaseMapper): ByteBuffer = ByteBuffer.wrap(v)
  }

  def afEnum[T](implicit et: EnumType[T]): AvroField[T] =
    new AvroField[T] {
      // Avro 1.9+ added a type parameter for `GenericEnumSymbol`, breaking 1.8 compatibility
      // Some reader, i.e. `AvroParquetReader` reads enums as `Utf8`
      override type FromT = AnyRef
      override type ToT = EnumSymbol

      override protected def buildSchema(cm: CaseMapper): Schema = {
        val doc = getDoc(et.annotations, s"Enum ${et.namespace}.${et.name}")
        Schema.createEnum(et.name, doc, et.namespace, et.values.asJava)
      }
      // `JacksonUtils.toJson` expects `String` for `ENUM` defaults
      override def makeDefault(d: T)(cm: CaseMapper): String = et.to(d)
      override def from(v: FromT)(cm: CaseMapper): T = et.from(v.toString)
      override def to(v: T)(cm: CaseMapper): ToT = new GenericData.EnumSymbol(schema(cm), v)
    }

  def afOption[T](implicit f: AvroField[T]): AvroField[Option[T]] =
    new AvroField.Aux[Option[T], f.FromT, f.ToT] {
      override protected def buildSchema(cm: CaseMapper): Schema =
        Schema.createUnion(Schema.create(Schema.Type.NULL), f.schema(cm))
      // `Option[T]` is a `UNION` of `[NULL, T]` and must default to first type `NULL`
      override def makeDefault(d: Option[T])(cm: CaseMapper): JsonProperties.Null = {
        require(d.isEmpty, "Option[T] can only default to None")
        JsonProperties.NULL_VALUE
      }
      override def from(v: f.FromT)(cm: CaseMapper): Option[T] =
        if (v == null) None else Some(f.from(v)(cm))
      override def to(v: Option[T])(cm: CaseMapper): f.ToT = v match {
        case None    => null.asInstanceOf[f.ToT]
        case Some(x) => f.to(x)(cm)
      }
    }

  def afIterable[T, C[_]](implicit
    f: AvroField[T],
    ti: C[T] => Iterable[T],
    fc: Factory[T, C[T]]
  ): AvroField[C[T]] =
    new AvroField.Aux[C[T], java.util.List[f.FromT], GenericArray[f.ToT]] {
      override protected def buildSchema(cm: CaseMapper): Schema =
        Schema.createArray(f.schema(cm))
      override val fallbackDefault: Any = java.util.Collections.emptyList()
      override def from(v: java.util.List[f.FromT])(cm: CaseMapper): C[T] = {
        val b = fc.newBuilder
        b ++= v.asScala.iterator.map(p => f.from(p)(cm))
        b.result()
      }

      override def to(v: C[T])(cm: CaseMapper): GenericArray[f.ToT] =
        new GenericData.Array[f.ToT](schema(cm), ti(v).iterator.map(f.to(_)(cm)).toList.asJava)
    }

  def afMap[T](implicit f: AvroField[T]): AvroField[Map[String, T]] =
    new AvroField.Aux[
      Map[String, T],
      java.util.Map[CharSequence, f.FromT],
      java.util.Map[String, f.ToT]
    ] {
      override protected def buildSchema(cm: CaseMapper): Schema =
        Schema.createMap(f.schema(cm))
      override val fallbackDefault: Any = java.util.Collections.emptyMap()
      override def from(v: java.util.Map[CharSequence, f.FromT])(cm: CaseMapper): Map[String, T] =
        v.asScala.iterator.map(kv => (kv._1.toString, f.from(kv._2)(cm))).toMap
      override def to(v: Map[String, T])(cm: CaseMapper): java.util.Map[String, f.ToT] =
        v.iterator.map(kv => (kv._1, f.to(kv._2)(cm))).toMap.asJava
    }

  // logical-types
  val afUuid: AvroField[UUID] =
    logicalType[String](LogicalTypes.uuid())(UUID.fromString)(_.toString)(afString)
  val afDate: AvroField[LocalDate] =
    logicalType[Int](LogicalTypes.date())(x => LocalDate.ofEpochDay(x.toLong))(
      _.toEpochDay.toInt
    )(afInt)

  // unsafe
  val afByte: AvroField[Byte] = from[Int](_.toByte)(_.toInt)(afInt)
  val afChar: AvroField[Char] = from[Int](_.toChar)(_.toInt)(afInt)
  val afShort: AvroField[Short] = from[Int](_.toShort)(_.toInt)(afInt)
  def afUnsafeEnum[T](implicit et: EnumType[T]): AvroField[UnsafeEnum[T]] =
    from[String](UnsafeEnum.from(_))(UnsafeEnum.to(_))(afString)
}

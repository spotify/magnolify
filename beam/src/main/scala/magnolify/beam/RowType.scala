/*
 * Copyright 2024 Spotify AB
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

package magnolify.beam

import magnolia1.*
import magnolify.shared.*
import org.apache.beam.sdk.schemas.Schema
import org.apache.beam.sdk.schemas.Schema.{Field, FieldType}
import org.apache.beam.sdk.values.Row
import com.google.protobuf.ByteString
import magnolify.shims.FactoryCompat
import org.apache.beam.sdk.schemas.logicaltypes

import java.nio.ByteBuffer
import java.util as ju
import scala.collection.compat.*
import scala.collection.concurrent
import scala.jdk.CollectionConverters.*

// https://beam.apache.org/documentation/programming-guide/#schema-definition
sealed trait RowType[T] extends Converter[T, Row, Row] {
  def schema: Schema
  def apply(r: Row): T = from(r)
  def apply(t: T): Row = to(t)
}

object RowType {
  implicit def apply[T: RowField]: RowType[T] = RowType[T](CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: RowField[T]): RowType[T] = {
    f match {
      case r: RowField.Record[_] =>
        val mappedSchema = r.schema(cm) // fail fast on bad annotations
        new RowType[T] {
          private val caseMapper: CaseMapper = cm
          override lazy val schema: Schema = mappedSchema

          override def from(v: Row): T = r.from(v)(caseMapper)
          override def to(v: T): Row = r.to(v)(caseMapper)
        }
      case _ =>
        throw new IllegalArgumentException(
          s"RowType can only be created from Record. Got $f"
        )
    }
  }
}

sealed trait RowField[T] extends Serializable {
  type FromT
  type ToT
  def fieldType(cm: CaseMapper): FieldType
  def from(v: FromT)(cm: CaseMapper): T
  def to(v: T)(cm: CaseMapper): ToT
  def fromAny(v: Any)(cm: CaseMapper): T = from(v.asInstanceOf[FromT])(cm)
}

object RowField {
  sealed trait Aux[T, From, To] extends RowField[T] {
    override type FromT = From
    override type ToT = To
  }

  private[magnolify] def aux[T, From, To](
    ft: CaseMapper => FieldType
  )(fromFn: From => T)(toFn: T => To): RowField[T] =
    new Aux[T, From, To] {
      override def fieldType(cm: CaseMapper): FieldType = ft(cm)
      override def from(v: FromT)(cm: CaseMapper): T = fromFn(v)
      override def to(v: T)(cm: CaseMapper): ToT = toFn(v)
    }

  private[magnolify] def id[T](ft: CaseMapper => FieldType): RowField[T] =
    aux[T, T, T](ft)(identity)(identity)

  def from[T]: FromWord[T] = new FromWord[T]

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit rf: RowField[T]): RowField[U] =
      new Aux[U, rf.FromT, rf.ToT] {
        override def fieldType(cm: CaseMapper): FieldType = rf.fieldType(cm)
        override def from(v: FromT)(cm: CaseMapper): U = f(rf.from(v)(cm))
        override def to(v: U)(cm: CaseMapper): ToT = rf.to(g(v))(cm)
      }
  }

  sealed trait Record[T] extends Aux[T, Row, Row] {
    @transient private lazy val schemaCache: concurrent.Map[ju.UUID, Schema] =
      concurrent.TrieMap.empty
    protected def buildSchema(cm: CaseMapper): Schema
    def schema(cm: CaseMapper): Schema = schemaCache.getOrElseUpdate(cm.uuid, buildSchema(cm))
  }

  // ////////////////////////////////////////////////

  type Typeclass[T] = RowField[T]
  implicit def gen[T]: RowField[T] = macro Magnolia.gen[T]

  def split[T](
    sealedTrait: SealedTrait[Typeclass, T]
  )(implicit r: shapeless.Refute[EnumType[T]]): RowField[T] =
    new RowField[T] {
      override type FromT = logicaltypes.OneOfType.Value
      override type ToT = logicaltypes.OneOfType.Value

      private def enumName(sub: Subtype[Typeclass, T]): String =
        s"${sub.typeName.owner}.${sub.typeName.short}"

      @transient private lazy val beamOneOfTypeCache
        : concurrent.Map[ju.UUID, logicaltypes.OneOfType] = concurrent.TrieMap.empty
      private def beamOneOfType(cm: CaseMapper): logicaltypes.OneOfType =
        beamOneOfTypeCache.getOrElseUpdate(
          cm.uuid,
          logicaltypes.OneOfType.create(
            sealedTrait.subtypes.map { sub =>
              Field.of(enumName(sub), sub.typeclass.fieldType(cm))
            }.asJava
          )
        )

      override def fieldType(cm: CaseMapper): FieldType =
        FieldType.logicalType(beamOneOfType(cm))
      def from(v: logicaltypes.OneOfType.Value)(cm: CaseMapper): T = {
        val idx = v.getCaseType.getValue
        sealedTrait.subtypes.find(_.index == idx) match {
          case None      => throw new IllegalArgumentException(s"OneOf index not found: [$idx]")
          case Some(sub) => sub.typeclass.fromAny(v.getValue)(cm)
        }
      }

      def to(v: T)(cm: CaseMapper): logicaltypes.OneOfType.Value =
        sealedTrait.split(v)(sub =>
          beamOneOfType(cm).createValue(enumName(sub), sub.typeclass.to(sub.cast(v))(cm))
        )
    }

  def join[T](caseClass: CaseClass[Typeclass, T]): RowField[T] = {
    if (caseClass.isValueClass) {
      val p = caseClass.parameters.head
      val tc = p.typeclass
      new RowField[T] {
        override type FromT = tc.FromT
        override type ToT = tc.ToT
        override def fieldType(cm: CaseMapper): FieldType = tc.fieldType(cm)
        override def from(v: FromT)(cm: CaseMapper): T = caseClass.construct(_ => tc.fromAny(v)(cm))
        override def to(v: T)(cm: CaseMapper): ToT = tc.to(p.dereference(v))(cm)
      }
    } else {
      new Record[T] {
        override def fieldType(cm: CaseMapper): FieldType = FieldType.row(schema(cm))

        override protected def buildSchema(cm: CaseMapper): Schema =
          caseClass.parameters
            .foldLeft(Schema.builder()) { case (s, p) =>
              s.addField(cm.map(p.label), p.typeclass.fieldType(cm))
            }
            .build()

        override def from(v: Row)(cm: CaseMapper): T =
          caseClass.construct(p => p.typeclass.fromAny(v.getValue[Any](p.index))(cm))

        override def to(v: T)(cm: CaseMapper): Row = {
          val values = caseClass.parameters.map { p =>
            p.typeclass.to(p.dereference(v))(cm).asInstanceOf[Object]
          }
          Row.withSchema(schema(cm)).addValues(values.asJava).build()
        }
      }
    }
  }

  // BYTE	An 8-bit signed value
  implicit val rfByte: RowField[Byte] = id[Byte](_ => FieldType.BYTE)
  // INT16	A 16-bit signed value
  implicit val rfShort: RowField[Short] = id[Short](_ => FieldType.INT16)
  implicit val rfChar: RowField[Char] = from[Short](_.toChar)(_.toShort)
  // INT32	A 32-bit signed value
  implicit val rfInt: RowField[Int] = id[Int](_ => FieldType.INT32)
  // INT64	A 64-bit signed value
  implicit val rfLong: RowField[Long] = id[Long](_ => FieldType.INT64)
  // FLOAT	A 32-bit IEEE 754 floating point number
  implicit val rfFloat: RowField[Float] = id[Float](_ => FieldType.FLOAT)
  // DOUBLE	A 64-bit IEEE 754 floating point number
  implicit val rfDouble: RowField[Double] = id[Double](_ => FieldType.DOUBLE)
  // STRING	A string
  implicit val rfString: RowField[String] = id[String](_ => FieldType.STRING)
  // BOOLEAN	A boolean value
  implicit val rfBoolean: RowField[Boolean] = id[Boolean](_ => FieldType.BOOLEAN)
  // BYTES	A raw byte array
  implicit val rfByteArray: RowField[Array[Byte]] = id[Array[Byte]](_ => FieldType.BYTES)
  implicit val rfByteBuffer: RowField[ByteBuffer] =
    from[Array[Byte]](x => ByteBuffer.wrap(x))(_.array())
  implicit val rfByteString: RowField[ByteString] =
    from[Array[Byte]](x => ByteString.copyFrom(x))(_.toByteArray)
  // DECIMAL	An arbitrary-precision decimal type
  implicit val rfDecimal: RowField[BigDecimal] =
    aux[BigDecimal, java.math.BigDecimal, java.math.BigDecimal](_ => FieldType.DECIMAL)(
      BigDecimal.apply
    )(_.bigDecimal)

  implicit val rfUUID: RowField[ju.UUID] =
    id[ju.UUID](_ => FieldType.logicalType(new logicaltypes.UuidLogicalType))

  implicit def rfEnum[T](implicit et: EnumType[T], lp: shapeless.LowPriority): RowField[T] =
    new RowField[T] {
      type FromT = logicaltypes.EnumerationType.Value
      type ToT = logicaltypes.EnumerationType.Value

      @transient private lazy val enumTypeCache: concurrent.Map[ju.UUID, EnumType[T]] =
        concurrent.TrieMap.empty
      @transient private lazy val beamEnumTypeCache
        : concurrent.Map[ju.UUID, logicaltypes.EnumerationType] =
        concurrent.TrieMap.empty

      private def enumType(cm: CaseMapper): EnumType[T] =
        enumTypeCache.getOrElseUpdate(cm.uuid, et.map(cm))
      private def beamEnumType(cm: CaseMapper): logicaltypes.EnumerationType =
        beamEnumTypeCache.getOrElseUpdate(
          cm.uuid,
          logicaltypes.EnumerationType.create(enumType(cm).values.asJava)
        )
      override def fieldType(cm: CaseMapper): FieldType = FieldType.logicalType(beamEnumType(cm))
      override def to(v: T)(cm: CaseMapper): ToT = beamEnumType(cm).valueOf(enumType(cm).to(v))
      override def from(v: FromT)(cm: CaseMapper): T =
        enumType(cm).from(beamEnumType(cm).toString(v))
    }

  implicit def rfMap[K, V](implicit rfK: RowField[K], rfV: RowField[V]): RowField[Map[K, V]] =
    new Aux[Map[K, V], ju.Map[rfK.FromT, rfV.FromT], ju.Map[rfK.ToT, rfV.ToT]] {
      override def fieldType(cm: CaseMapper): FieldType =
        FieldType.map(rfK.fieldType(cm), rfV.fieldType(cm))
      override def from(v: ju.Map[rfK.FromT, rfV.FromT])(cm: CaseMapper): Map[K, V] =
        v.asScala.map { case (k, v) => rfK.from(k)(cm) -> rfV.from(v)(cm) }.toMap
      override def to(v: Map[K, V])(cm: CaseMapper): ju.Map[rfK.ToT, rfV.ToT] =
        v.map { case (k, v) => rfK.to(k)(cm) -> rfV.to(v)(cm) }.asJava
    }

  implicit def rfIterable[T, C[_]](implicit
    f: RowField[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): RowField[C[T]] = {
    new Aux[C[T], ju.List[f.FromT], ju.List[f.ToT]] {
      override def from(v: ju.List[f.FromT])(cm: CaseMapper): C[T] =
        fc.fromSpecific(v.asScala.iterator.map(p => f.from(p)(cm)))
      override def to(v: C[T])(cm: CaseMapper): ju.List[f.ToT] =
        v.iterator.map(f.to(_)(cm)).toList.asJava
      override def fieldType(cm: CaseMapper): FieldType = FieldType.iterable(f.fieldType(cm))
    }
  }

  implicit def rfOption[T](implicit f: RowField[T]): RowField[Option[T]] = {
    new Aux[Option[T], f.FromT, f.ToT] {
      override def from(v: f.FromT)(cm: CaseMapper): Option[T] =
        if (v == null) None else Some(f.from(v)(cm))
      override def to(v: Option[T])(cm: CaseMapper): f.ToT = v match {
        case None    => null.asInstanceOf[f.ToT]
        case Some(x) => f.to(x)(cm)
      }
      override def fieldType(cm: CaseMapper): FieldType = f.fieldType(cm).withNullable(true)
    }
  }
}

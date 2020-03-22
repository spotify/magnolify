/*
 * Copyright 2020 Spotify AB.
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
package magnolify.bigtable

import java.nio.ByteBuffer

import com.google.bigtable.v2.Mutation
import com.google.bigtable.v2.Mutation.SetCell
import com.google.cloud.bigtable.data.v2.models.{Row, RowCell}
import com.google.protobuf.ByteString
import magnolia._
import magnolify.shared.Converter
import magnolify.shims.JavaConverters._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

sealed trait BigtableType[T] extends Converter[T, Row, Seq[Mutation.Builder]] {
  protected val columnFamily: String
  protected val timestampMicros: Long

  def apply(v: Row): T = from(v)
  def apply(v: T): Seq[Mutation] = to(v).map(_.build())
}

object BigtableType {
  val DEFAULT_COLUMN_FAMILY_NAME = "f"
  val DEFAULT_COLUMN_QUALIFIER_NAME = "q"

  implicit def apply[T](implicit f: BigtableField[T]): BigtableType[T] =
    BigtableType(BigtableType.DEFAULT_COLUMN_FAMILY_NAME, 0L)

  def apply[T](_columnFamily: String, _timestampMicros: Long)(
    implicit f: BigtableField[T]
  ): BigtableType[T] = new BigtableType[T] {
    override protected val columnFamily: String = _columnFamily
    override protected val timestampMicros: Long = _timestampMicros

    override def from(v: Row): T = f.get(columnFamily, v, null)

    override def to(v: T): Seq[Mutation.Builder] =
      f.put(null, v).map { b =>
        Mutation
          .newBuilder()
          .setSetCell(b.setFamilyName(columnFamily).setTimestampMicros(timestampMicros))
      }
  }

  def mutationsToRow(key: ByteString, mutations: Seq[Mutation]): Row =
    Row.create(
      key,
      mutations
        .sortBy(_.getSetCell.getColumnQualifier.toStringUtf8)
        .map(_.getSetCell)
        .map { setCell =>
          RowCell.create(
            setCell.getFamilyName,
            setCell.getColumnQualifier,
            setCell.getTimestampMicros,
            List.empty.asJava,
            setCell.getValue
          )
        }
        .asJava
    )

  def rowToMutations(row: Row): Seq[Mutation] =
    row.getCells.asScala.iterator.map { cell =>
      Mutation
        .newBuilder()
        .setSetCell(
          SetCell
            .newBuilder()
            .setFamilyName(cell.getFamily)
            .setColumnQualifier(cell.getQualifier)
            .setTimestampMicros(cell.getTimestamp)
            .setValue(cell.getValue)
        )
        .build()
    }.toSeq
}

sealed trait BigtableField[T] extends Serializable {
  def get(columnFamily: String, v: Row, k: String): T
  def put(k: String, v: T): Seq[SetCell.Builder]
}

object BigtableField {
  trait Primitive[T] extends BigtableField[T] {
    def fromByteString(v: ByteString): T
    def toByteString(v: T): ByteString

    private def columnQualifier(k: String): ByteString =
      ByteString.copyFromUtf8(if (k != null) k else BigtableType.DEFAULT_COLUMN_QUALIFIER_NAME)

    override def get(columnFamily: String, r: Row, k: String): T = {
      val cq = columnQualifier(k)
      val cells = r.getCells(columnFamily, cq)
      require(cells.size() == 1)
      fromByteString(cells.get(0).getValue)
    }

    override def put(k: String, v: T): Seq[SetCell.Builder] =
      Seq(
        SetCell
          .newBuilder()
          .setColumnQualifier(columnQualifier(k))
          .setValue(toByteString(v))
      )
  }

  //////////////////////////////////////////////////

  type Typeclass[T] = BigtableField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): BigtableField[T] = new BigtableField[T] {
    private def key(prefix: String, label: String): String =
      if (prefix == null) label else s"$prefix.$label"

    override def get(columnFamily: String, f: Row, k: String): T =
      caseClass.construct(p => p.typeclass.get(columnFamily, f, key(k, p.label)))

    override def put(k: String, v: T): Seq[SetCell.Builder] =
      caseClass.parameters.flatMap(p => p.typeclass.put(key(k, p.label), p.dereference(v)))
  }

  @implicitNotFound("Cannot derive BigtableField for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): BigtableField[T] = ???

  implicit def gen[T]: BigtableField[T] = macro Magnolia.gen[T]

  def apply[T](implicit f: BigtableField[T]): BigtableField[T] = f

  def from[T]: FromWord[T] = new FromWord

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit btf: Primitive[T]): Primitive[U] =
      new Primitive[U] {
        def fromByteString(v: ByteString): U = f(btf.fromByteString(v))
        def toByteString(v: U): ByteString = btf.toByteString(g(v))
      }
  }

  private def primitive[T](
    capacity: Int
  )(f: ByteBuffer => T)(g: (ByteBuffer, T) => Unit): Primitive[T] = new Primitive[T] {
    override def fromByteString(v: ByteString): T = f(v.asReadOnlyByteBuffer())
    override def toByteString(v: T): ByteString = {
      val bb = ByteBuffer.allocate(capacity)
      g(bb, v)
      bb.rewind()
      ByteString.copyFrom(bb)
    }
  }

  implicit val btfInt = primitive[Int](java.lang.Integer.BYTES)(_.getInt)(_.putInt(_))
  implicit val btfLong = primitive[Long](java.lang.Long.BYTES)(_.getLong)(_.putLong(_))
  implicit val btfFloat = primitive[Float](java.lang.Float.BYTES)(_.getFloat)(_.putFloat(_))
  implicit val btfDouble = primitive[Double](java.lang.Double.BYTES)(_.getDouble)(_.putDouble(_))
  implicit val btfByte = primitive[Byte](java.lang.Byte.BYTES)(_.get)(_.put(_))
  implicit val btfShort = primitive[Short](java.lang.Short.BYTES)(_.getShort)(_.putShort(_))
  implicit val btfBoolean = from[Byte](_ == 1)(if (_) 1 else 0)

  implicit val btfByteString = new Primitive[ByteString] {
    override def fromByteString(v: ByteString): ByteString = v
    override def toByteString(v: ByteString): ByteString = v
  }
  implicit val btfByteArray = from[ByteString](_.toByteArray)(ByteString.copyFrom(_))
  implicit val btfString = from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8(_))

  implicit def btfOption[A](implicit btf: BigtableField[A]): BigtableField[Option[A]] =
    new BigtableField[Option[A]] {
      override def get(columnFamily: String, v: Row, k: String): Option[A] = {
        val isOption = v.getCells(columnFamily).asScala.exists { c =>
          val q = c.getQualifier.toStringUtf8
          q == k || q.startsWith(s"$k.") // optional primitive or nested field
        }
        if (isOption) Some(btf.get(columnFamily, v, k)) else None
      }

      override def put(k: String, vo: Option[A]): Seq[SetCell.Builder] = vo match {
        case None    => Seq.empty
        case Some(v) => btf.put(k, v)
      }
    }
}

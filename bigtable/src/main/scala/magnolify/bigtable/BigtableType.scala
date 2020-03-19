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

import com.google.protobuf.ByteString
import magnolia._

import scala.collection._
import com.google.bigtable.v2.Mutation
import com.google.bigtable.v2.Mutation.SetCell
import com.google.cloud.bigtable.data.v2.models.{Row, RowCell}

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import magnolify.shims.JavaConverters._

sealed trait BigtableType[T] extends Serializable {

  def from(v: Row, columnFamily: String = BigtableType.DEFAULT_COLUMN_FAMILY_NAME): T

  def to(v: T, columnFamily: String = BigtableType.DEFAULT_COLUMN_FAMILY_NAME,
               timestamp: Long = 0L,
               columnQualifier: String = null): Iterable[Mutation]

}

object BigtableType {
  val DEFAULT_COLUMN_QUALIFIER_NAME = "q"
  val DEFAULT_COLUMN_FAMILY_NAME = "f"

  implicit def apply[T](implicit f: BigtableField[T]): BigtableType[T] = new BigtableType[T] {

    def from(v: Row, columnFamily: String = BigtableType.DEFAULT_COLUMN_FAMILY_NAME): T =
      f.get(columnFamily, v, null)

    def to(v: T, columnFamily: String = BigtableType.DEFAULT_COLUMN_FAMILY_NAME,
                 timestamp: Long = 0L,
                 columnQualifier: String = null): Iterable[Mutation] =
      f.put(columnQualifier, v).map(Mutations.newSetCellMutation(columnFamily, timestamp))

  }
}

sealed trait BigtableField[T] extends Serializable {
  def get(columnFamily: String, v: Row, k: String): T
  def put(k: String, v: T): Iterable[SetCell.Builder]
}

object BigtableField {
  trait Primitive[T] extends BigtableField[T] {
    type ValueT
    def fromByteString(v: ByteString): T
    def toByteString(v: T): ByteString

    def get(columnFamily: String, f: Row, k: String): T = {
      val columnQualifier = Option(k).getOrElse(BigtableType.DEFAULT_COLUMN_QUALIFIER_NAME)
      val cells = f.getCells(columnFamily, columnQualifier)
      require(cells.size() == 1)
      fromByteString(cells.get(0).getValue)
    }

    def put(k: String, v: T): Iterable[SetCell.Builder] =
      Iterable(Mutations.newSetCell(
        ByteString.copyFromUtf8(Option(k).getOrElse(BigtableType.DEFAULT_COLUMN_QUALIFIER_NAME)),
        toByteString(v))
      )
  }

  type Typeclass[T] = BigtableField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): BigtableField[T] = new BigtableField[T] {
    private def key(prefix: String, label: String): String =
      if (prefix == null) label else s"$prefix.$label"

    def get(columnFamily: String, f: Row, k: String): T =
      caseClass.construct(p => p.typeclass.get(columnFamily, f, key(k, p.label)))

    def put(k: String, v: T): Iterable[SetCell.Builder] =
      caseClass.parameters.flatMap { p =>
        p.typeclass.put(key(k, p.label), p.dereference(v))
      }
  }

  @implicitNotFound("Cannot derive BigtableField for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): BigtableField[T] = ???

  implicit def gen[T]: BigtableField[T] = macro Magnolia.gen[T]

  def apply[T](implicit f: BigtableField[T]): BigtableField[T] = f

  def from[T]: FromWord[T] = new FromWord

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit ef: Primitive[T]): Primitive[U] =
      new Primitive[U] {
        type ValueT = ef.ValueT
        def fromByteString(v: ByteString): U = f(ef.fromByteString(v))
        def toByteString(v: U): ByteString = ef.toByteString(g(v))
      }
  }

  private def writeByteString(capacity: Int,
                              writer: ByteBuffer => Unit): ByteString = {
    val byteBuffer = ByteBuffer.allocate(capacity)
    writer(byteBuffer)
    byteBuffer.rewind()

    ByteString.copyFrom(byteBuffer)
  }

  implicit val btLong = new Primitive[Long] {
    type ValueT = Long

    def fromByteString(v: ByteString): Long =
      v.asReadOnlyByteBuffer().asLongBuffer().get()

    def toByteString(v: Long): ByteString =
      writeByteString(java.lang.Long.BYTES, _.putLong(v))
  }

  implicit val btInt = new Primitive[Int] {
    type ValueT = Int

    def fromByteString(v: ByteString): Int =
      v.asReadOnlyByteBuffer().asIntBuffer().get()

    def toByteString(v: Int): ByteString =
      writeByteString(java.lang.Integer.BYTES, _.putInt(v))
  }

  implicit val btFloat = new Primitive[Float] {
    type ValueT = Float

    def fromByteString(v: ByteString): Float =
      v.asReadOnlyByteBuffer().asFloatBuffer().get()

    def toByteString(v: Float): ByteString =
      writeByteString(java.lang.Float.BYTES, _.putFloat(v))
  }

  implicit val btDouble = new Primitive[Double] {
    type ValueT = Double

    def fromByteString(v: ByteString): Double =
      v.asReadOnlyByteBuffer().asDoubleBuffer().get()

    def toByteString(v: Double): ByteString =
      writeByteString(java.lang.Double.BYTES, _.putDouble(v))
  }

  implicit val btShort = new Primitive[Short] {
    type ValueT = Short

    def fromByteString(v: ByteString): Short =
      v.asReadOnlyByteBuffer().asShortBuffer().get()

    def toByteString(v: Short): ByteString =
      writeByteString(java.lang.Float.BYTES, _.putShort(v))
  }

  implicit val btString = new Primitive[String] {
    type ValueT = String

    def fromByteString(v: ByteString): String =
      v.toStringUtf8

    def toByteString(v: String): ByteString =
      ByteString.copyFromUtf8(v)
  }

  implicit val btBoolean = new Primitive[Boolean] {
      type ValueT = Boolean

      def fromByteString(v: ByteString): Boolean =
         v.asReadOnlyByteBuffer().asShortBuffer().get() match {
           case 0 => false
           case _ => true
         }

      def toByteString(v: Boolean): ByteString =
        writeByteString(java.lang.Float.BYTES, _.putShort(if(v) 1 else 0))
  }

  implicit def btOption[A](implicit btField: BigtableField[A]) = new BigtableField[Option[A]] {
    private def hasNestedField(columnFamily: String, r: Row, k: String): Boolean =
      r.getCells(columnFamily).asScala.iterator.exists(_.getQualifier.toStringUtf8.startsWith(s"$k."))

    def get(columnFamily: String, v: Row, k: String): Option[A] = {
      if (v.getCells(columnFamily, k).size() > 0 || hasNestedField(columnFamily, v, k))
        Option(btField.get(columnFamily, v, k))
      else
        Option.empty
    }

    def put(k: String, vo: Option[A]): Iterable[SetCell.Builder]
    = vo match {
      case None    => Iterable.empty
      case Some(v) => btField.put(k, v)
    }
  }

}
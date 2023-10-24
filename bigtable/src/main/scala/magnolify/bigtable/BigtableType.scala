/*
 * Copyright 2020 Spotify AB
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

package magnolify.bigtable

import com.google.bigtable.v2.Mutation.SetCell
import com.google.bigtable.v2.*
import com.google.protobuf.ByteString
import magnolia1.*
import magnolify.shared.*
import magnolify.shims.*

import java.nio.ByteBuffer
import java.util.UUID
import scala.annotation.implicitNotFound
import scala.jdk.CollectionConverters.*

sealed trait BigtableType[T] extends Converter[T, Map[String, Column], Seq[SetCell.Builder]] {
  def apply(v: Row, columnFamily: String): T =
    from(
      v.getFamiliesList.asScala
        .find(_.getName == columnFamily)
        .map(_.getColumnsList.asScala.map(c => c.getQualifier.toStringUtf8 -> c).toMap)
        .getOrElse(Map.empty)
    )
  def apply(v: T, columnFamily: String, timestampMicros: Long = 0L): Seq[Mutation] =
    to(v).map { b =>
      Mutation
        .newBuilder()
        .setSetCell(b.setFamilyName(columnFamily).setTimestampMicros(timestampMicros))
        .build()
    }
}

object BigtableType {
  implicit def apply[T: BigtableField]: BigtableType[T] = BigtableType(CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: BigtableField[T]): BigtableType[T] = f match {
    case r: BigtableField.Record[_] =>
      new BigtableType[T] {
        private val caseMapper: CaseMapper = cm
        override def from(xs: Map[String, Column]): T = r.get(xs, null)(caseMapper)
        override def to(v: T): Seq[SetCell.Builder] = r.put(null, v)(caseMapper)
      }
    case _ =>
      throw new IllegalArgumentException(s"BigtableType can only be created from Record. Got $f")
  }

  def mutationsToRow(key: ByteString, mutations: Seq[Mutation]): Row = {
    val families = mutations
      .map(_.getSetCell)
      .groupBy(_.getFamilyName)
      .map { case (familyName, setCells) =>
        val columns = setCells
          .sortBy(_.getColumnQualifier.toStringUtf8)
          .map { setCell =>
            Column
              .newBuilder()
              .setQualifier(setCell.getColumnQualifier)
              .addCells(
                Cell
                  .newBuilder()
                  .setValue(setCell.getValue)
                  .setTimestampMicros(setCell.getTimestampMicros)
              )
              .build()
          }
        Family.newBuilder().setName(familyName).addAllColumns(columns.asJava).build()
      }
      .toSeq
    Row.newBuilder().setKey(key).addAllFamilies(families.asJava).build()
  }

  def rowToMutations(row: Row): Seq[Mutation] =
    for {
      family <- row.getFamiliesList.asScala.toSeq
      column <- family.getColumnsList.asScala
      cell <- column.getCellsList.asScala
    } yield Mutation
      .newBuilder()
      .setSetCell(
        SetCell
          .newBuilder()
          .setFamilyName(family.getName)
          .setColumnQualifier(column.getQualifier)
          .setTimestampMicros(cell.getTimestampMicros)
          .setValue(cell.getValue)
      )
      .build()
}

sealed trait BigtableField[T] extends Serializable {
  def get(xs: Map[String, Column], k: String)(cm: CaseMapper): T
  def put(k: String, v: T)(cm: CaseMapper): Seq[SetCell.Builder]
}

object BigtableField {

  private def key(prefix: String, label: String): String =
    if (prefix == null) label else s"$prefix.$label"

  private def columnFilter(key: String): (String, Column) => Boolean = {
    val recordKey = key + "."
    (name: String, _: Column) => name == key || name.startsWith(recordKey)
  }

  sealed trait Record[T] extends BigtableField[T]

  sealed trait Primitive[T] extends BigtableField[T] {
    val size: Option[Int]
    def fromByteString(v: ByteString): T
    def toByteString(v: T): ByteString

    private def columnQualifier(k: String): ByteString = ByteString.copyFromUtf8(k)

    override def get(xs: Map[String, Column], k: String)(cm: CaseMapper): T =
      fromByteString(xs(k).getCells(0).getValue)

    override def put(k: String, v: T)(cm: CaseMapper): Seq[SetCell.Builder] =
      Seq(
        SetCell
          .newBuilder()
          .setColumnQualifier(columnQualifier(k))
          .setValue(toByteString(v))
      )
  }

  // ////////////////////////////////////////////////

  type Typeclass[T] = BigtableField[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): BigtableField[T] = {
    if (caseClass.isValueClass) {
      val p = caseClass.parameters.head
      val tc = p.typeclass
      new BigtableField[T] {
        override def get(xs: Map[String, Column], k: String)(cm: CaseMapper): T =
          caseClass.construct(_ => tc.get(xs, k)(cm))
        override def put(k: String, v: T)(cm: CaseMapper): Seq[SetCell.Builder] =
          p.typeclass.put(k, p.dereference(v))(cm)
      }
    } else {
      new Record[T] {
        override def get(xs: Map[String, Column], k: String)(cm: CaseMapper): T = {
          caseClass.construct { p =>
            val qualifier = key(k, cm.map(p.label))
            val columns = xs.filter(columnFilter(qualifier).tupled)
            // consider default value only if all fields are missing
            p.default
              .filter(_ => columns.isEmpty)
              .getOrElse(p.typeclass.get(columns, qualifier)(cm))
          }
        }

        override def put(k: String, v: T)(cm: CaseMapper): Seq[SetCell.Builder] =
          caseClass.parameters.flatMap { p =>
            p.typeclass.put(key(k, cm.map(p.label)), p.dereference(v))(cm)
          }
      }
    }
  }

  @implicitNotFound("Cannot derive BigtableField for sealed trait")
  private sealed trait Dispatchable[T]
  def split[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): BigtableField[T] = ???

  implicit def gen[T]: BigtableField[T] = macro Magnolia.gen[T]

  def apply[T](implicit f: BigtableField[T]): BigtableField[T] = f

  def from[T]: FromWord[T] = new FromWord

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit btf: Primitive[T]): Primitive[U] =
      new Primitive[U] {
        override val size: Option[Int] = btf.size
        def fromByteString(v: ByteString): U = f(btf.fromByteString(v))
        def toByteString(v: U): ByteString = btf.toByteString(g(v))
      }
  }

  private def primitive[T](
    capacity: Int
  )(f: ByteBuffer => T)(g: (ByteBuffer, T) => ByteBuffer): Primitive[T] = new Primitive[T] {
    override val size: Option[Int] = Some(capacity)
    override def fromByteString(v: ByteString): T = f(v.asReadOnlyByteBuffer())
    override def toByteString(v: T): ByteString = {
      val bb = ByteBuffer.allocate(capacity)
      ByteString.copyFrom(g(bb, v).array())
    }
  }

  implicit val btfByte: Primitive[Byte] = primitive[Byte](java.lang.Byte.BYTES)(_.get)(_.put(_))
  implicit val btChar: Primitive[Char] =
    primitive[Char](java.lang.Character.BYTES)(_.getChar)(_.putChar(_))
  implicit val btfShort: Primitive[Short] =
    primitive[Short](java.lang.Short.BYTES)(_.getShort)(_.putShort(_))
  implicit val btfInt: Primitive[Int] =
    primitive[Int](java.lang.Integer.BYTES)(_.getInt)(_.putInt(_))
  implicit val btfLong: Primitive[Long] =
    primitive[Long](java.lang.Long.BYTES)(_.getLong)(_.putLong(_))
  implicit val btfFloat: Primitive[Float] =
    primitive[Float](java.lang.Float.BYTES)(_.getFloat)(_.putFloat(_))
  implicit val btfDouble: Primitive[Double] =
    primitive[Double](java.lang.Double.BYTES)(_.getDouble)(_.putDouble(_))
  implicit val btfBoolean: Primitive[Boolean] = from[Byte](_ == 1)(if (_) 1 else 0)
  implicit val btfUUID: Primitive[UUID] =
    primitive[UUID](16)(bb => new UUID(bb.getLong, bb.getLong)) { (bb, uuid) =>
      bb.putLong(uuid.getMostSignificantBits).putLong(uuid.getLeastSignificantBits)
    }

  implicit val btfByteString: Primitive[ByteString] = new Primitive[ByteString] {
    override val size: Option[Int] = None
    override def fromByteString(v: ByteString): ByteString = v
    override def toByteString(v: ByteString): ByteString = v
  }
  implicit val btfByteArray: Primitive[Array[Byte]] =
    from[ByteString](_.toByteArray)(ByteString.copyFrom)
  implicit val btfString: Primitive[String] =
    from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8)

  implicit def btfEnum[T](implicit et: EnumType[T]): Primitive[T] =
    from[String](et.from)(et.to)

  implicit def btfUnsafeEnum[T: EnumType]: Primitive[UnsafeEnum[T]] =
    from[String](UnsafeEnum.from[T])(UnsafeEnum.to[T])

  implicit val btfBigInt: Primitive[BigInt] =
    from[ByteString](bs => BigInt(bs.toByteArray))(bi => ByteString.copyFrom(bi.toByteArray))
  implicit val btfBigDecimal: Primitive[BigDecimal] = from[ByteString] { bs =>
    val bb = bs.asReadOnlyByteBuffer()
    val scale = bb.getInt
    val unscaled = BigInt(bs.substring(java.lang.Integer.BYTES).toByteArray)
    BigDecimal.apply(unscaled, scale)
  } { bd =>
    val scale = bd.bigDecimal.scale()
    val unscaled = bd.bigDecimal.unscaledValue().toByteArray
    val bb = ByteBuffer.allocate(java.lang.Integer.BYTES + unscaled.length)
    ByteString.copyFrom(bb.putInt(scale).put(unscaled).array())
  }

  implicit def btfOption[T](implicit btf: BigtableField[T]): BigtableField[Option[T]] =
    new BigtableField[Option[T]] {
      override def get(xs: Map[String, Column], k: String)(cm: CaseMapper): Option[T] =
        if (xs.isEmpty) None else Some(btf.get(xs, k)(cm))

      override def put(k: String, v: Option[T])(cm: CaseMapper): Seq[SetCell.Builder] =
        v.toSeq.flatMap(btf.put(k, _)(cm))
    }

  implicit def btfIterable[T, C[T]](implicit
    btf: Primitive[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): Primitive[C[T]] =
    new Primitive[C[T]] {
      override val size: Option[Int] = None

      override def fromByteString(v: ByteString): C[T] = {
        val buf = v.asReadOnlyByteBuffer()
        val n = buf.getInt
        val b = fc.newBuilder
        btf.size match {
          case Some(size) =>
            val ba = new Array[Byte](size)
            (1 to n).foreach { _ =>
              buf.get(ba)
              b += btf.fromByteString(ByteString.copyFrom(ba))
            }
          case None =>
            (1 to n).foreach { _ =>
              val s = buf.getInt
              val ba = new Array[Byte](s)
              buf.get(ba)
              b += btf.fromByteString(ByteString.copyFrom(ba))
            }
        }
        b.result()
      }

      override def toByteString(v: C[T]): ByteString = {
        val buf = btf.size match {
          case Some(size) =>
            val bb = ByteBuffer.allocate(java.lang.Integer.BYTES + v.size * size)
            bb.putInt(v.size)
            v.foreach(x => bb.put(btf.toByteString(x).asReadOnlyByteBuffer()))
            bb
          case None =>
            val vs = v.map(btf.toByteString)
            val size = java.lang.Integer.BYTES * (v.size + 1) + vs.iterator.map(_.size()).sum
            val bb = ByteBuffer.allocate(size)
            bb.putInt(v.size)
            vs.foreach { v =>
              bb.putInt(v.size())
              bb.put(v.asReadOnlyByteBuffer())
            }
            bb
        }
        ByteString.copyFrom(buf.array())
      }
    }
}

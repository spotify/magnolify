/*
 * Copyright 2023 Spotify AB
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

package magnolify.hbase

import magnolia1.{CaseClass, Magnolia, SealedTrait}
import magnolify.shared.*
import magnolify.shims.FactoryCompat
import org.apache.hadoop.hbase.client.{Mutation, Put, Result}

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import scala.annotation.implicitNotFound
import scala.jdk.CollectionConverters.*
sealed trait HbaseType[T] extends Converter[T, Map[String, Array[Byte]], Map[String, Array[Byte]]] {
  def apply(v: Result, family: Array[Byte]): T =
    from(
      v.getFamilyMap(family)
        .asScala
        .map { case (qualifier, value) => new String(qualifier, UTF_8) -> value }
        .toMap
    )

  def apply(v: T, row: Array[Byte], family: Array[Byte], ts: Long = 0L): Mutation = to(v)
    .foldLeft(new Put(row)) { case (row, (qualifier, value)) =>
      row.addColumn(family, qualifier.getBytes(UTF_8), ts, value)
    }
}

object HbaseType {
  implicit def apply[T: HbaseField]: HbaseType[T] = HbaseType(CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: HbaseField[T]): HbaseType[T] = f match {
    case r: HbaseField.Record[_] =>
      new HbaseType[T] {
        private val caseMapper: CaseMapper = cm

        override def from(xs: Map[String, Array[Byte]]): T = r.get(xs, null)(caseMapper).get

        override def to(v: T): Map[String, Array[Byte]] = r.put(null, v)(caseMapper)
      }
    case _ =>
      throw new IllegalArgumentException(s"BigtableType can only be created from Record. Got $f")
  }
}

sealed trait HbaseField[T] extends Serializable {
  def get(xs: Map[String, Array[Byte]], k: String)(cm: CaseMapper): Value[T]
  def put(k: String, v: T)(cm: CaseMapper): Map[String, Array[Byte]]
}

object HbaseField {
  sealed trait Record[T] extends HbaseField[T]

  sealed trait Primitive[T] extends HbaseField[T] {
    def size: Option[Int]
    def fromBytes(v: Array[Byte]): T
    def toBytes(v: T): Array[Byte]

    override def get(xs: Map[String, Array[Byte]], k: String)(cm: CaseMapper): Value[T] =
      xs.get(k) match {
        case Some(v) => Value.Some(fromBytes(v))
        case None    => Value.None
      }

    override def put(k: String, v: T)(cm: CaseMapper): Map[String, Array[Byte]] =
      Map(k -> toBytes(v))
  }

  // ////////////////////////////////////////////////
  type Typeclass[T] = HbaseField[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): HbaseField[T] = {
    if (caseClass.isValueClass) {
      val p = caseClass.parameters.head
      val tc = p.typeclass
      new HbaseField[T] {
        override def get(xs: Map[String, Array[Byte]], k: String)(cm: CaseMapper): Value[T] =
          tc.get(xs, k)(cm).map(x => caseClass.construct(_ => x))

        override def put(k: String, v: T)(cm: CaseMapper): Map[String, Array[Byte]] =
          p.typeclass.put(k, p.dereference(v))(cm)
      }
    } else {
      new Record[T] {
        private def qualifier(prefix: String, label: String): String =
          if (prefix == null) label else s"$prefix.$label"

        override def get(xs: Map[String, Array[Byte]], k: String)(cm: CaseMapper): Value[T] = {
          var fallback = true
          val r = caseClass.construct { p =>
            val q = qualifier(k, cm.map(p.label))
            val v = p.typeclass.get(xs, q)(cm)
            if (v.isSome) {
              fallback = false
            }
            v.getOrElse(p.default)
          }
          // result is default if all fields are default
          if (fallback) Value.Default(r) else Value.Some(r)
        }

        override def put(k: String, v: T)(cm: CaseMapper): Map[String, Array[Byte]] =
          caseClass.parameters.flatMap { p =>
            p.typeclass.put(qualifier(k, cm.map(p.label)), p.dereference(v))(cm)
          }.toMap
      }
    }
  }

  @implicitNotFound("Cannot derive BigtableField for sealed trait")
  private sealed trait Dispatchable[T]

  def split[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): HbaseField[T] = ???

  implicit def gen[T]: HbaseField[T] = macro Magnolia.gen[T]

  def apply[T](implicit f: HbaseField[T]): HbaseField[T] = f

  def from[T]: FromWord[T] = new FromWord

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit hbf: Primitive[T]): Primitive[U] =
      new Primitive[U] {
        override def size: Option[Int] = hbf.size
        override def fromBytes(v: Array[Byte]): U = f(hbf.fromBytes(v))
        override def toBytes(v: U): Array[Byte] = hbf.toBytes(g(v))
      }
  }

  private def primitive[T](
    capacity: Int
  )(f: ByteBuffer => T)(g: (ByteBuffer, T) => ByteBuffer): Primitive[T] = new Primitive[T] {
    override val size: Option[Int] = Some(capacity)
    override def fromBytes(v: Array[Byte]): T = f(ByteBuffer.wrap(v).asReadOnlyBuffer())
    override def toBytes(v: T): Array[Byte] = g(ByteBuffer.allocate(capacity), v).array()
  }

  implicit val hbfByte: Primitive[Byte] =
    primitive[Byte](java.lang.Byte.BYTES)(_.get)(_.put(_))
  implicit val btChar: Primitive[Char] =
    primitive[Char](java.lang.Character.BYTES)(_.getChar)(_.putChar(_))
  implicit val hbfShort: Primitive[Short] =
    primitive[Short](java.lang.Short.BYTES)(_.getShort)(_.putShort(_))
  implicit val hbfInt: Primitive[Int] =
    primitive[Int](java.lang.Integer.BYTES)(_.getInt)(_.putInt(_))
  implicit val hbfLong: Primitive[Long] =
    primitive[Long](java.lang.Long.BYTES)(_.getLong)(_.putLong(_))
  implicit val hbfFloat: Primitive[Float] =
    primitive[Float](java.lang.Float.BYTES)(_.getFloat)(_.putFloat(_))
  implicit val hbfDouble: Primitive[Double] =
    primitive[Double](java.lang.Double.BYTES)(_.getDouble)(_.putDouble(_))
  implicit val hbfBoolean: Primitive[Boolean] =
    from[Byte](_ == 1)(if (_) 1 else 0)
  implicit val hbfUUID: Primitive[UUID] =
    primitive[UUID](16)(bb => new UUID(bb.getLong, bb.getLong)) { (bb, uuid) =>
      bb.putLong(uuid.getMostSignificantBits).putLong(uuid.getLeastSignificantBits)
    }

  implicit val hbfByteString: Primitive[Array[Byte]] = new Primitive[Array[Byte]] {
    override val size: Option[Int] = None
    override def fromBytes(v: Array[Byte]): Array[Byte] = v
    override def toBytes(v: Array[Byte]): Array[Byte] = v
  }

  implicit val hbfString: Primitive[String] =
    from[Array[Byte]](new String(_, UTF_8))(_.getBytes(UTF_8))
  implicit def hbfEnum[T](implicit et: EnumType[T]): Primitive[T] =
    from[String](et.from)(et.to)
  implicit def hbfUnsafeEnum[T: EnumType]: Primitive[UnsafeEnum[T]] =
    from[String](UnsafeEnum.from[T])(UnsafeEnum.to[T])

  implicit val hbfBigInt: Primitive[BigInt] =
    from[Array[Byte]](bs => BigInt(bs))(_.toByteArray)
  implicit val hbfBigDecimal: Primitive[BigDecimal] = from[Array[Byte]] { bs =>
    val bb = ByteBuffer.wrap(bs).asReadOnlyBuffer()
    val scale = bb.getInt
    val unscaled = new Array[Byte](bb.remaining())
    bb.get(unscaled)
    BigDecimal(BigInt(unscaled), scale)
  } { bd =>
    val scale = bd.bigDecimal.scale()
    val unscaled = bd.bigDecimal.unscaledValue().toByteArray
    val bb = ByteBuffer.allocate(java.lang.Integer.BYTES + unscaled.length)
    bb.putInt(scale).put(unscaled).array()
  }

  implicit def hbfOption[T](implicit hbf: HbaseField[T]): HbaseField[Option[T]] =
    new HbaseField[Option[T]] {
      override def get(xs: Map[String, Array[Byte]], k: String)(
        cm: CaseMapper
      ): Value[Option[T]] = {
        val subset = xs.filter { case (qualifier, _) => qualifier.startsWith(k) }
        if (subset.isEmpty) Value.Default(None) else hbf.get(subset, k)(cm).toOption
      }

      override def put(k: String, v: Option[T])(cm: CaseMapper): Map[String, Array[Byte]] =
        v.map(hbf.put(k, _)(cm)).getOrElse(Map.empty)
    }

  implicit def hbfIterable[T, C[T]](implicit
    hbf: Primitive[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): Primitive[C[T]] =
    new Primitive[C[T]] {
      override val size: Option[Int] = None

      override def fromBytes(v: Array[Byte]): C[T] = {
        val buf = ByteBuffer.wrap(v).asReadOnlyBuffer()
        val n = buf.getInt
        val b = fc.newBuilder
        hbf.size match {
          case Some(size) =>
            val ba = new Array[Byte](size)
            (1 to n).foreach { _ =>
              buf.get(ba)
              b += hbf.fromBytes(ba)
            }
          case None =>
            (1 to n).foreach { _ =>
              val s = buf.getInt
              val ba = new Array[Byte](s)
              buf.get(ba)
              b += hbf.fromBytes(ba)
            }
        }
        b.result()
      }

      override def toBytes(v: C[T]): Array[Byte] = {
        val buf = hbf.size match {
          case Some(size) =>
            val bb = ByteBuffer.allocate(java.lang.Integer.BYTES + v.size * size)
            bb.putInt(v.size)
            v.foreach(x => bb.put(hbf.toBytes(x)))
            bb
          case None =>
            val vs = v.map(hbf.toBytes)
            val size =
              java.lang.Integer.BYTES + vs.iterator.map(_.length + java.lang.Integer.BYTES).sum
            val bb = ByteBuffer.allocate(size)
            bb.putInt(v.size)
            vs.foreach { v =>
              bb.putInt(v.length)
              bb.put(v)
            }
            bb
        }
        buf.array()
      }
    }
}

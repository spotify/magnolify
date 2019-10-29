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
package magnolify.tensorflow

import java.lang.{Iterable => JIterable}
import java.util.{List => JList}

import com.google.protobuf.ByteString
import magnolia._
import magnolify.shared.Converter
import magnolify.shims.FactoryCompat
import org.tensorflow.example._

import scala.collection.JavaConverters._
import scala.language.experimental.macros

sealed trait ExampleType[T] extends Converter[T, Features, Features.Builder] {
  def apply(r: ExampleOrBuilder): T = from(r.getFeatures)
  def apply(t: T): Example = Example.newBuilder().setFeatures(to(t)).build()
  override protected def empty: Features.Builder = Features.newBuilder()
  override def from(r: Features): T = ???
  override def to(t: T): Features.Builder = ???
}

object ExampleType {
  type Typeclass[T] = ExampleField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override val kind: ExampleField.Kind = null
    override val nested: Boolean = true

    override def from(r: Features): T =
      caseClass.construct { p =>
        if (p.typeclass.nested) {
          // FIXME: optimize this to avoid copies
          val inner = empty
          val prefix = p.label + '.'
          r.getFeatureMap.asScala.foreach {
            case (k, v) =>
              if (k.startsWith(prefix)) {
                inner.putFeature(k.substring(prefix.length), v)
              }
          }
          p.typeclass.fromField(inner.build())
        } else {
          p.typeclass.get(r, p.label)
        }
      }

    override def to(t: T): Features.Builder =
      caseClass.parameters.foldLeft(empty) { (r, p) =>
        if (p.typeclass.nested) {
          // FIXME: optimize this to avoid copies
          val inner = p.typeclass.toField(p.dereference(t)).asInstanceOf[Features.Builder]
          val prefix = p.label + '.'
          inner.getFeatureMap.asScala.foreach {
            case (k, v) =>
              r.putFeature(prefix + k, v)
          }
          r
        } else {
          p.typeclass.put(r, p.label, p.dereference(t))
        }
      }

    override def fromField(v: Any): T =
      caseClass.construct(p => p.typeclass.get(v.asInstanceOf[Features], p.label))
    override def toField(v: T): Any =
      caseClass.parameters.foldLeft(empty) { (r, p) =>
        p.typeclass.put(r, p.label, p.dereference(v))
      }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: ExampleType[T] = macro Magnolia.gen[T]
}

sealed trait ExampleField[V] extends ExampleType[V] { self =>
  val kind: ExampleField.Kind
  val nested: Boolean = false

  def get(r: Features, k: String): V = {
    val xs = kind.getList(r.getFeatureMap.get(k))
    require(xs.size == 1)
    fromField(xs.iterator().next())
  }
  def put(r: Features.Builder, k: String, v: V): Features.Builder =
    r.putFeature(k, kind.putList(Seq(toField(v))).build())

  def fromField(v: Any): V
  def toField(v: V): Any

  def imap[U](f: V => U)(g: U => V): ExampleField[U] = new ExampleField[U] {
    override val kind: ExampleField.Kind = self.kind
    override def fromField(v: Any): U = f(self.fromField(v))
    override def toField(v: U): Any = self.toField(g(v))
  }
}

object ExampleField {
  def apply[V](implicit f: ExampleField[V]): ExampleField[V] = f

  private def atSingle[V](k: Kind): ExampleField[V] = new ExampleField[V] {
    override val kind: Kind = k
    override def fromField(v: Any): V = v.asInstanceOf[V]
    override def toField(v: V): Any = v.asInstanceOf[Any]
  }

  def atLong[V](f: Long => V)(g: V => Long): ExampleField[V] = efLong.imap(f)(g)
  def atFloat[V](f: Float => V)(g: V => Float): ExampleField[V] = efFloat.imap(f)(g)
  def atBytes[V](f: ByteString => V)(g: V => ByteString): ExampleField[V] = efBytes.imap(f)(g)

  sealed abstract class Kind(
    val kind: Feature.KindCase,
    val getFn: Feature => JList[Any],
    val putFn: Feature.Builder => Iterable[Any] => Feature.Builder
  ) extends Serializable {
    def getList(v: Feature): JList[Any] = getFn(v)
    def putList(v: Iterable[Any]): Feature.Builder = putFn(Feature.newBuilder())(v)
  }

  object Kind {
    case object Long
        extends Kind(
          Feature.KindCase.INT64_LIST,
          _.getInt64List.getValueList.asInstanceOf[JList[Any]],
          b =>
            xs =>
              b.setInt64List(
                Int64List
                  .newBuilder()
                  .addAllValue(xs.asJava.asInstanceOf[JIterable[java.lang.Long]])
              )
        )

    case object Float
        extends Kind(
          Feature.KindCase.FLOAT_LIST,
          _.getFloatList.getValueList.asInstanceOf[JList[Any]],
          b =>
            xs =>
              b.setFloatList(
                FloatList
                  .newBuilder()
                  .addAllValue(xs.asJava.asInstanceOf[JIterable[java.lang.Float]])
              )
        )

    case object Bytes
        extends Kind(
          Feature.KindCase.BYTES_LIST,
          _.getBytesList.getValueList.asInstanceOf[JList[Any]],
          b =>
            xs =>
              b.setBytesList(
                BytesList.newBuilder().addAllValue(xs.asJava.asInstanceOf[JIterable[ByteString]])
              )
        )
  }

  implicit val efLong = atSingle[Long](Kind.Long)
  implicit val efFloat = atSingle[Float](Kind.Float)
  implicit val efBytes = atSingle[ByteString](Kind.Bytes)

  implicit def efOption[V](implicit f: ExampleField[V]): ExampleField[Option[V]] =
    new ExampleField[Option[V]] {
      override val kind: Kind = f.kind
      override def fromField(v: Any): Option[V] = ???
      override def toField(v: Option[V]): Any = ???

      override def get(r: Features, k: String): Option[V] = r.getFeatureMap.get(k) match {
        case null => None
        case v: Feature =>
          val xs = kind.getList(v)
          require(xs.size <= 1)
          if (xs.isEmpty) None else Some(f.fromField(xs.iterator().next()))
      }
      override def put(r: Features.Builder, k: String, v: Option[V]): Features.Builder =
        r.putFeature(k, kind.putList(v.map(f.toField)).build())
    }

  implicit def efSeq[V, S[V]](
    implicit f: ExampleField[V],
    ts: S[V] => Seq[V],
    fc: FactoryCompat[V, S[V]]
  ): ExampleField[S[V]] =
    new ExampleField[S[V]] {
      override val kind: Kind = f.kind
      override def fromField(v: Any): S[V] = ???
      override def toField(v: S[V]): Any = ???

      override def get(r: Features, k: String): S[V] = r.getFeatureMap.get(k) match {
        case null       => fc.newBuilder.result()
        case v: Feature => fc.build(kind.getList(v).asScala.map(f.fromField))
      }
      override def put(r: Features.Builder, k: String, v: S[V]): Features.Builder =
        r.putFeature(k, kind.putList(v.map(f.toField)).build())
    }
}

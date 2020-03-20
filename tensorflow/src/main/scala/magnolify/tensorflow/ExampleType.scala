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

import java.{lang => jl, util => ju}

import com.google.protobuf.ByteString
import magnolia._
import magnolify.shared.Converter
import magnolify.shims.FactoryCompat
import magnolify.shims.JavaConverters._
import org.tensorflow.example._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

sealed trait ExampleType[T] extends Converter[T, Example, Example.Builder] {
  def apply(v: Example): T = from(v)
  def apply(v: T): Example = to(v).build()
}

object ExampleType {
  implicit def apply[T](implicit f: ExampleField.Record[T]): ExampleType[T] = new ExampleType[T] {
    override def from(v: Example): T = f.get(v.getFeatures, null)
    override def to(v: T): Example.Builder =
      Example.newBuilder().setFeatures(f.put(Features.newBuilder(), null, v))
  }
}

sealed trait ExampleField[T] extends Serializable {
  def get(f: Features, k: String): T
  def put(f: Features.Builder, k: String, v: T): Features.Builder
}

object ExampleField {
  trait Primitive[T] extends ExampleField[T] {
    type ValueT
    def fromFeature(v: Feature): ju.List[T]
    def toFeature(v: Iterable[T]): Feature

    override def get(f: Features, k: String): T = {
      val l = fromFeature(f.getFeatureOrDefault(k, null))
      require(l.size() == 1)
      l.get(0)
    }

    override def put(f: Features.Builder, k: String, v: T): Features.Builder =
      f.putFeature(k, toFeature(Iterable(v)))
  }

  trait Record[T] extends ExampleField[T]

  //////////////////////////////////////////////////

  type Typeclass[T] = ExampleField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    private def key(prefix: String, label: String): String =
      if (prefix == null) label else s"$prefix.$label"

    override def get(f: Features, k: String): T =
      caseClass.construct(p => p.typeclass.get(f, key(k, p.label)))

    override def put(f: Features.Builder, k: String, v: T): Features.Builder =
      caseClass.parameters.foldLeft(f) { (f, p) =>
        p.typeclass.put(f, key(k, p.label), p.dereference(v))
        f
      }
  }

  @implicitNotFound("Cannot derive ExampleField for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Record[T] = ???

  implicit def gen[T]: Record[T] = macro Magnolia.gen[T]

  //////////////////////////////////////////////////

  def apply[T](implicit f: ExampleField[T]): ExampleField[T] = f

  def from[T]: FromWord[T] = new FromWord

  class FromWord[T] {
    def apply[U](f: T => U)(g: U => T)(implicit ef: Primitive[T]): Primitive[U] =
      new Primitive[U] {
        override type ValueT = ef.ValueT
        override def fromFeature(v: Feature): ju.List[U] =
          ef.fromFeature(v).asScala.map(f).asJava
        override def toFeature(v: Iterable[U]): Feature = ef.toFeature(v.map(g))
      }
  }

  implicit val efLong = new Primitive[Long] {
    override type ValueT = jl.Long
    override def fromFeature(v: Feature): ju.List[Long] =
      if (v == null) {
        java.util.Collections.emptyList()
      } else {
        v.getInt64List.getValueList.asInstanceOf[ju.List[Long]]
      }

    override def toFeature(v: Iterable[Long]): Feature =
      Feature
        .newBuilder()
        .setInt64List(Int64List.newBuilder().addAllValue(v.asInstanceOf[Iterable[jl.Long]].asJava))
        .build()
  }

  implicit val efFloat = new Primitive[Float] {
    override type ValueT = jl.Float
    override def fromFeature(v: Feature): ju.List[Float] =
      if (v == null) {
        java.util.Collections.emptyList()
      } else {
        v.getFloatList.getValueList.asInstanceOf[ju.List[Float]]
      }

    override def toFeature(v: Iterable[Float]): Feature =
      Feature
        .newBuilder()
        .setFloatList(FloatList.newBuilder().addAllValue(v.asInstanceOf[Iterable[jl.Float]].asJava))
        .build()
  }

  implicit val efBytes = new Primitive[ByteString] {
    override type ValueT = ByteString
    override def fromFeature(v: Feature): ju.List[ByteString] =
      if (v == null) {
        java.util.Collections.emptyList()
      } else {
        v.getBytesList.getValueList
      }

    override def toFeature(v: Iterable[ByteString]): Feature =
      Feature
        .newBuilder()
        .setBytesList(BytesList.newBuilder().addAllValue(v.asJava))
        .build()
  }

  implicit def efOption[T](implicit ef: ExampleField[T]): ExampleField[Option[T]] =
    new ExampleField[Option[T]] {
      override def get(f: Features, k: String): Option[T] =
        if (f.containsFeature(k) || f.getFeatureMap.keySet().asScala.exists(_.startsWith(s"$k."))) {
          Some(ef.get(f, k))
        } else {
          None
        }

      override def put(f: Features.Builder, k: String, v: Option[T]): Features.Builder = v match {
        case None    => f
        case Some(x) => ef.put(f, k, x)
      }
    }

  implicit def efIterable[T, C[_]](
    implicit ef: Primitive[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): ExampleField[C[T]] = new ExampleField[C[T]] {
    override def get(f: Features, k: String): C[T] =
      fc.build(ef.fromFeature(f.getFeatureOrDefault(k, null)).asScala)

    override def put(f: Features.Builder, k: String, v: C[T]): Features.Builder =
      if (v.isEmpty) f else f.putFeature(k, ef.toFeature(v))
  }
}

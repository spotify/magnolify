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
import magnolify.shared.{CaseMapper, Converter}
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
  implicit def apply[T: ExampleField.Record]: ExampleType[T] = ExampleType(CaseMapper.identity)

  def apply[T](cm: CaseMapper)(implicit f: ExampleField.Record[T]): ExampleType[T] =
    new ExampleType[T] {
      private val caseMapper: CaseMapper = cm
      override def from(v: Example): T = f.get(v.getFeatures, null)(caseMapper).get
      override def to(v: T): Example.Builder =
        Example.newBuilder().setFeatures(f.put(Features.newBuilder(), null, v)(caseMapper))
    }
}

sealed trait Result[+T] {
  def get: T = this match {
    case Result.Feature(v) => v
    case Result.Default(v) => v
    case Result.None       => throw new NoSuchElementException
  }
}

private object Result {
  // value from `Feature`
  case class Feature[T](value: T) extends Result[T]

  // value from case class default
  case class Default[T](value: T) extends Result[T]

  // value not found
  case object None extends Result[Nothing]
}

sealed trait ExampleField[T] extends Serializable {
  def get(f: Features, k: String)(cm: CaseMapper): Result[T]
  def put(f: Features.Builder, k: String, v: T)(cm: CaseMapper): Features.Builder
}

object ExampleField {
  trait Primitive[T] extends ExampleField[T] {
    type ValueT
    def fromFeature(v: Feature): ju.List[T]
    def toFeature(v: Iterable[T]): Feature

    override def get(f: Features, k: String)(cm: CaseMapper): Result[T] = {
      val feature = f.getFeatureOrDefault(k, null)
      if (feature == null) {
        Result.None
      } else {
        val l = fromFeature(feature)
        require(l.size() == 1)
        Result.Feature(l.get(0))
      }
    }

    override def put(f: Features.Builder, k: String, v: T)(cm: CaseMapper): Features.Builder =
      f.putFeature(k, toFeature(Iterable(v)))
  }

  trait Record[T] extends ExampleField[T]

  //////////////////////////////////////////////////

  type Typeclass[T] = ExampleField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Record[T] = new Record[T] {
    private def key(prefix: String, label: String): String =
      if (prefix == null) label else s"$prefix.$label"

    override def get(f: Features, k: String)(cm: CaseMapper): Result[T] = {
      var fallback = true
      val r = caseClass.construct { p =>
        val fk = key(k, cm.map(p.label))
        (p.typeclass.get(f, fk)(cm), p.default) match {
          case (Result.Feature(x), _) =>
            fallback = false
            x
          case (Result.Default(_), Some(x)) => x
          case (Result.Default(x), None)    => x
          case (Result.None, Some(x))       => x
          case _ =>
            throw new IllegalArgumentException(s"Feature not found: $fk")
        }
      }
      // result is default if all fields are default
      if (fallback) Result.Default(r) else Result.Feature(r)
    }

    override def put(f: Features.Builder, k: String, v: T)(cm: CaseMapper): Features.Builder =
      caseClass.parameters.foldLeft(f) { (f, p) =>
        p.typeclass.put(f, key(k, cm.map(p.label)), p.dereference(v))(cm)
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

  implicit val efByteString = new Primitive[ByteString] {
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

  implicit val efByteArray = from[ByteString](_.toByteArray)(ByteString.copyFrom(_))

  implicit def efOption[T](implicit ef: ExampleField[T]): ExampleField[Option[T]] =
    new ExampleField[Option[T]] {
      override def get(f: Features, k: String)(cm: CaseMapper): Result[Option[T]] =
        if (f.containsFeature(k) || f.getFeatureMap.keySet().asScala.exists(_.startsWith(s"$k."))) {
          ef.get(f, k)(cm) match {
            case Result.Feature(x) => Result.Feature(Some(x))
            case Result.Default(x) => Result.Default(Some(x))
            case Result.None       => Result.Default(None)
          }
        } else {
          Result.Default(None)
        }

      override def put(f: Features.Builder, k: String, v: Option[T])(
        cm: CaseMapper
      ): Features.Builder = v match {
        case None    => f
        case Some(x) => ef.put(f, k, x)(cm)
      }
    }

  implicit def efIterable[T, C[_]](implicit
    ef: Primitive[T],
    ti: C[T] => Iterable[T],
    fc: FactoryCompat[T, C[T]]
  ): ExampleField[C[T]] = new ExampleField[C[T]] {
    override def get(f: Features, k: String)(cm: CaseMapper): Result[C[T]] = {
      val v = f.getFeatureOrDefault(k, null)
      if (v == null) Result.Default(fc.build(Nil))
      else Result.Feature(fc.build(ef.fromFeature(v).asScala))
    }

    override def put(f: Features.Builder, k: String, v: C[T])(cm: CaseMapper): Features.Builder =
      if (v.isEmpty) f else f.putFeature(k, ef.toFeature(v))
  }
}

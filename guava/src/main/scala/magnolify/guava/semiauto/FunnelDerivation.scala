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

package magnolify.guava.semiauto

import com.google.common.hash.{Funnel, Funnels, PrimitiveSink}
import magnolia1.*

import java.nio.charset.StandardCharsets

object FunnelDerivation {
  type Typeclass[T] = Funnel[T]

  def join[T](caseClass: ReadOnlyCaseClass[Typeclass, T]): Typeclass[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit =
      if (caseClass.isValueClass) {
        val p = caseClass.parameters.head
        p.typeclass.funnel(p.dereference(from), into)
      } else if (caseClass.parameters.isEmpty) {
        val _ = into.putString(caseClass.typeName.short, StandardCharsets.UTF_8)
      } else {
        caseClass.parameters.foreach { p =>
          // inject index to distinguish cases like `(Some(1), None)` and `(None, Some(1))`
          into.putInt(p.index)
          p.typeclass.funnel(p.dereference(from), into)
        }
      }
  }

  def split[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit =
      sealedTrait.split(from)(sub => sub.typeclass.funnel(sub.cast(from), into))
  }

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]

  def by[T, S](f: T => S)(implicit fnl: Funnel[S]): Funnel[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit = fnl.funnel(f(from), into)
  }
}

trait FunnelImplicits {
  private def funnel[T](f: (PrimitiveSink, T) => Any): Funnel[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit = {
      val _ = f(into, from)
    }
  }

  implicit val intFunnel: Funnel[Int] = Funnels.integerFunnel().asInstanceOf[Funnel[Int]]
  implicit val longFunnel: Funnel[Long] = Funnels.longFunnel().asInstanceOf[Funnel[Long]]
  implicit val bytesFunnel: Funnel[Array[Byte]] = Funnels.byteArrayFunnel()
  implicit val booleanFunnel: Funnel[Boolean] = funnel[Boolean](_.putBoolean(_))
  implicit val byteFunnel: Funnel[Byte] = funnel[Byte](_.putByte(_))
  implicit val charFunnel: Funnel[Char] = funnel[Char](_.putChar(_))
  implicit val shortFunnel: Funnel[Short] = funnel[Short](_.putShort(_))

  implicit def charSequenceFunnel[T <: CharSequence]: Funnel[T] =
    Funnels.unencodedCharsFunnel().asInstanceOf[Funnel[T]]

  // There is an implicit Option[T] => Iterable[T]
  implicit def iterableFunnel[T, C[_]](implicit
    fnl: Funnel[T],
    ti: C[T] => Iterable[T]
  ): Funnel[C[T]] =
    funnel { (sink, from) =>
      var i = 0
      from.foreach { x =>
        fnl.funnel(x, sink)
        i += 1
      }
      // inject size to distinguish `None`, `Some("")`, and `List("", "", ...)`
      sink.putInt(i)
    }
}

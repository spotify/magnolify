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
package magnolify.guava.semiauto

import com.google.common.base.Charsets
import com.google.common.hash.{Funnel, Funnels, PrimitiveSink}
import magnolia._

import scala.language.experimental.macros

object FunnelDerivation {
  type Typeclass[T] = Funnel[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit =
      if (caseClass.parameters.isEmpty) {
        into.putString(caseClass.typeName.short, Charsets.UTF_8)
      } else {
        caseClass.parameters.foreach { p =>
          // inject index to distinguish cases like `(Some(1), None)` and `(None, Some(1))`
          into.putInt(p.index)
          p.typeclass.funnel(p.dereference(from), into)
        }
      }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit =
      sealedTrait.dispatch(from)(sub => sub.typeclass.funnel(sub.cast(from), into))
  }

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]

  def by[T, S](f: T => S)(implicit fnl: Funnel[S]): Funnel[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit = fnl.funnel(f(from), into)
  }
}

trait FunnelImplicits {
  private def funnel[T](f: (PrimitiveSink, T) => Unit): Funnel[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit = f(into, from)
  }

  implicit val intFunnel: Funnel[Int] = Funnels.integerFunnel().asInstanceOf[Funnel[Int]]
  implicit val longFunnel: Funnel[Long] = Funnels.longFunnel().asInstanceOf[Funnel[Long]]
  implicit val bytesFunnel: Funnel[Array[Byte]] = Funnels.byteArrayFunnel()
  implicit val charSequenceFunnel: Funnel[CharSequence] = Funnels.unencodedCharsFunnel()

  implicit val booleanFunnel: Funnel[Boolean] = funnel[Boolean](_.putBoolean(_))
  implicit val stringFunnel: Funnel[String] = funnel[String](_.putString(_, Charsets.UTF_8))
  implicit val byteFunnel: Funnel[Byte] = funnel[Byte](_.putByte(_))
  implicit val charFunnel: Funnel[Char] = funnel[Char](_.putChar(_))
  implicit val shortFunnel: Funnel[Short] = funnel[Short](_.putShort(_))

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

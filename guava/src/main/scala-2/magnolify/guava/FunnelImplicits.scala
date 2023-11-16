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

package magnolify.guava

import com.google.common.hash.{Funnel, PrimitiveSink}

trait FunnelImplicits {
  import FunnelImplicits.*
  def Funnel[T](implicit fnl: Funnel[T]): Funnel[T] = fnl

  implicit val intFunnel: Funnel[Int] = FunnelInstances.intFunnel()
  implicit val longFunnel: Funnel[Long] = FunnelInstances.longFunnel()
  implicit val bytesFunnel: Funnel[Array[Byte]] = FunnelInstances.bytesFunnel()
  implicit val booleanFunnel: Funnel[Boolean] = FunnelInstances.booleanFunnel()
  implicit val byteFunnel: Funnel[Byte] = FunnelInstances.byteFunnel()
  implicit val charFunnel: Funnel[Char] = FunnelInstances.charFunnel()
  implicit val shortFunnel: Funnel[Short] = FunnelInstances.shortFunnel()

  implicit def charSequenceFunnel[T <: CharSequence]: Funnel[T] =
    FunnelInstances.charSequenceFunnel[T]

  // There is an implicit Option[T] => Iterable[T]
  implicit def iterableFunnel[T, C[_]](implicit
    fnl: Funnel[T],
    ti: C[T] => Iterable[T]
  ): Funnel[C[T]] = FunnelInstances.iterableFunnel(fnl).contramap(ti)

  implicit def funnelOps[T](fnl: Funnel[T]): FunnelOps[T] = new FunnelOps(fnl)
}

object FunnelImplicits extends FunnelImplicits {
  final class FunnelOps[T](val fnl: Funnel[T]) extends AnyVal {
    def contramap[U](f: U => T): Funnel[U] = new Funnel[U] {
      override def funnel(from: U, into: PrimitiveSink): Unit = fnl.funnel(f(from), into)
    }
  }
}

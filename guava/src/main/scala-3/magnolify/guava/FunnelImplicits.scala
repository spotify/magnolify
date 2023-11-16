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

trait FunnelImplicits:
  def Funnel[T](using fnl: Funnel[T]): Funnel[T] = fnl

  given intFunnel: Funnel[Int] = FunnelInstances.intFunnel()
  given longFunnel: Funnel[Long] = FunnelInstances.longFunnel()
  given bytesFunnel: Funnel[Array[Byte]] = FunnelInstances.bytesFunnel()
  given booleanFunnel: Funnel[Boolean] = FunnelInstances.booleanFunnel()
  given byteFunnel: Funnel[Byte] = FunnelInstances.byteFunnel()
  given charFunnel: Funnel[Char] = FunnelInstances.charFunnel()
  given shortFunnel: Funnel[Short] = FunnelInstances.shortFunnel()

  given charSequenceFunnel[T <: CharSequence]: Funnel[T] =
    FunnelInstances.charSequenceFunnel[T]

  // There is an implicit Option[T] => Iterable[T]
  given iterableFunnel[T, C[_]](using
    fnl: Funnel[T],
    ti: C[T] => Iterable[T]
  ): Funnel[C[T]] = FunnelInstances.iterableFunnel(fnl).contramap(ti)

  extension [T](fnl: Funnel[T])
    def contramap[U](f: U => T): Funnel[U] = new Funnel[U]:
      override def funnel(from: U, into: PrimitiveSink): Unit = fnl.funnel(f(from), into)

object FunnelImplicits extends FunnelImplicits

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

import com.google.common.hash.{Funnel, Funnels, PrimitiveSink}

import scala.annotation.nowarn

object FunnelInstances {

  private def funnel[T](f: (PrimitiveSink, T) => Any): Funnel[T] = new Funnel[T] {
    override def funnel(from: T, into: PrimitiveSink): Unit = f(into, from): @nowarn
  }

  // respect naming convention from Funnels
  def intFunnel(): Funnel[Int] = Funnels.integerFunnel().asInstanceOf[Funnel[Int]]
  def longFunnel(): Funnel[Long] = Funnels.longFunnel().asInstanceOf[Funnel[Long]]
  def bytesFunnel(): Funnel[Array[Byte]] = Funnels.byteArrayFunnel()
  def booleanFunnel(): Funnel[Boolean] = funnel[Boolean](_.putBoolean(_))
  def byteFunnel(): Funnel[Byte] = funnel[Byte](_.putByte(_))
  def charFunnel(): Funnel[Char] = funnel[Char](_.putChar(_))
  def shortFunnel(): Funnel[Short] = funnel[Short](_.putShort(_))

  def charSequenceFunnel[T <: CharSequence]: Funnel[T] =
    Funnels.unencodedCharsFunnel().asInstanceOf[Funnel[T]]
  def iterableFunnel[T](fnl: Funnel[T]): Funnel[Iterable[T]] =
    funnel { (sink, xs) =>
      var i = 0
      xs.foreach { x =>
        fnl.funnel(x, sink)
        i += 1
      }
      // inject size to distinguish `None`, `Some("")`, and `List("", "", ...)`
      sink.putInt(i)
    }
}

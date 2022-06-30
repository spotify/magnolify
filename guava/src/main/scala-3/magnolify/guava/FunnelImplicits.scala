/*
 * Copyright 2022 Spotify AB
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

import com.google.common.base.Charsets
import com.google.common.hash.{Funnel, PrimitiveSink}

trait FunnelImplicits:
  given intFunnel: Funnel[Int] = Funnels.intFunnel
  given longFunnel: Funnel[Long] = Funnels.longFunnel
  given bytesFunnel: Funnel[Array[Byte]] = Funnels.bytesFunnel
  given charSequenceFunnel: Funnel[CharSequence] = Funnels.charSequenceFunnel
  given booleanFunnel: Funnel[Boolean] = Funnels.booleanFunnel
  given stringFunnel: Funnel[String] = Funnels.stringFunnel
  given byteFunnel: Funnel[Byte] = Funnels.byteFunnel
  given charFunnel: Funnel[Char] = Funnels.charFunnel
  given shortFunnel: Funnel[Short] = Funnels.shortFunnel
  given iterableFunnel[T, C[_]](using Funnel[T], C[T] => Iterable[T]): Funnel[C[T]] =
    Funnels.iterableFunnel

object FunnelImplicits extends FunnelImplicits

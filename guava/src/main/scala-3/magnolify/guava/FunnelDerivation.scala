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

package magnolify.guava

import com.google.common.base.Charsets
import com.google.common.hash.{Funnel, PrimitiveSink}
import magnolia1.*

import scala.annotation.nowarn
import scala.deriving.Mirror

object FunnelDerivation extends Derivation[Funnel]:

  def join[T](caseClass: CaseClass[Funnel, T]): Funnel[T] = new Funnel[T]:
    override def funnel(from: T, into: PrimitiveSink): Unit =
      if (caseClass.isValueClass)
        val p = caseClass.parameters.head
        p.typeclass.funnel(p.deref(from), into)
      else if (caseClass.parameters.isEmpty)
        into.putString(caseClass.typeInfo.short, Charsets.UTF_8): @nowarn
      else
        caseClass.parameters.foreach { p =>
          // inject index to distinguish cases like `(Some(1), None)` and `(None, Some(1))`
          into.putInt(p.index)
          p.typeclass.funnel(p.deref(from), into)
        }

  def split[T](sealedTrait: SealedTrait[Funnel, T]): Funnel[T] = new Funnel[T]:
    override def funnel(from: T, into: PrimitiveSink): Unit =
      sealedTrait.choose(from)(sub => sub.typeclass.funnel(sub.cast(from), into))

  inline def gen[T](using Mirror.Of[T]): Funnel[T] = derivedMirror[T]

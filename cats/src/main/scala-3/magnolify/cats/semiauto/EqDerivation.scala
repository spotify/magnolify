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

package magnolify.cats.semiauto

import cats.Eq
import magnolia1.*

import scala.deriving.Mirror

object EqDerivation extends Derivation[Eq]:

  def join[T](caseClass: CaseClass[Eq, T]): Eq[T] =
    Eq.instance(EqMethods.join(caseClass))

  def split[T](sealedTrait: SealedTrait[Eq, T]): Eq[T] =
    Eq.instance(EqMethods.split(sealedTrait))

  inline given apply[T](using Mirror.Of[T]): Eq[T] = derived[T]
end EqDerivation

private object EqMethods:

  def join[T, Typeclass[T] <: Eq[T]](
    caseClass: CaseClass[Typeclass, T]
  ): (T, T) => Boolean =
    (x, y) => caseClass.params.forall(p => p.typeclass.eqv(p.deref(x), p.deref(y)))

  def split[T, Typeclass[T] <: Eq[T]](
    sealedTrait: SealedTrait[Typeclass, T]
  ): (T, T) => Boolean =
    (x, y) =>
      sealedTrait.choose(x) { sub =>
        sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.value, sub.cast(y))
      }

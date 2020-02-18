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
package magnolify.cats.semiauto

import cats.Eq
import magnolia._

import scala.language.experimental.macros

object EqDerivation {
  type Typeclass[T] = Eq[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    Eq.instance(EqMethods.combine(caseClass))

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    Eq.instance(EqMethods.dispatch(sealedTrait))

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]
}

private object EqMethods {
  def combine[T, Typeclass[T] <: Eq[T]](caseClass: CaseClass[Typeclass, T]): (T, T) => Boolean =
    (x, y) => caseClass.parameters.forall(p => p.typeclass.eqv(p.dereference(x), p.dereference(y)))

  def dispatch[T, Typeclass[T] <: Eq[T]](
    sealedTrait: SealedTrait[Typeclass, T]
  ): (T, T) => Boolean =
    (x, y) =>
      sealedTrait.dispatch(x) { sub =>
        sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.cast(x), sub.cast(y))
      }
}

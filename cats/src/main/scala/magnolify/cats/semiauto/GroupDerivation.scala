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

import cats.Group
import magnolia._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

object GroupDerivation {
  type Typeclass[T] = Group[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = {
    val emptyImpl = MonoidMethods.empty(caseClass)
    val combineImpl = SemigroupMethods.combine(caseClass)
    val combineNImpl = SemigroupMethods.combineN(caseClass)
    val combineAllOptionImpl = SemigroupMethods.combineAllOption(caseClass)

    new Group[T] {
      override def empty: T = emptyImpl
      override def combine(x: T, y: T): T = combineImpl(x, y)
      override def combineN(x: T, n: Int): T =
        if (n > 0) {
          combineNImpl(x, n)
        } else if (n == 0) {
          emptyImpl
        } else if (n == Int.MinValue) {
          combineN(inverse(combine(x, x)), 1073741824)
        } else {
          combineNImpl(inverse(x), -n)
        }
      override def combineAllOption(as: TraversableOnce[T]): Option[T] = combineAllOptionImpl(as)

      override def inverse(a: T): T = caseClass.construct { p =>
        p.typeclass.inverse(p.dereference(a))
      }

      override def remove(a: T, b: T): T = caseClass.construct { p =>
        p.typeclass.remove(p.dereference(a), p.dereference(b))
      }
    }
  }

  @implicitNotFound("Cannot derive Group for sealed trait")
  private sealed trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]
}

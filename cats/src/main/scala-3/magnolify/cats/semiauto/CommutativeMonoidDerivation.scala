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

import cats.kernel.CommutativeMonoid
import magnolia1.*

import scala.deriving.Mirror

object CommutativeMonoidDerivation extends ProductDerivation[CommutativeMonoid]:

  def join[T](caseClass: CaseClass[CommutativeMonoid, T]): CommutativeMonoid[T] =
    val emptyImpl = MonoidMethods.empty(caseClass)
    val combineImpl = SemigroupMethods.combine(caseClass)
    val combineNImpl = MonoidMethods.combineN(caseClass)
    val combineAllImpl = MonoidMethods.combineAll(caseClass)
    val combineAllOptionImpl = SemigroupMethods.combineAllOption(caseClass)

    new CommutativeMonoid[T]:
      override def empty: T = emptyImpl()
      override def combine(x: T, y: T): T = combineImpl(x, y)
      override def combineN(a: T, n: Int): T = combineNImpl(a, n)
      override def combineAll(as: IterableOnce[T]): T = combineAllImpl(as)
      override def combineAllOption(as: IterableOnce[T]): Option[T] = combineAllOptionImpl(as)
  end join

  inline def apply[T](using Mirror.Of[T]): CommutativeMonoid[T] = derivedMirror[T]
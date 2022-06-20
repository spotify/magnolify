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

import cats.Monoid
import magnolia1.*

import scala.collection.immutable.ArraySeq.unsafeWrapArray
import scala.deriving.Mirror

object MonoidDerivation extends ProductDerivation[Monoid]:

  def join[T](caseClass: CaseClass[Monoid, T]): Monoid[T] = {
    val emptyImpl = MonoidMethods.empty(caseClass)
    val combineImpl = SemigroupMethods.combine(caseClass)
    val combineNImpl = MonoidMethods.combineN(caseClass)
    val combineAllImpl = MonoidMethods.combineAll(caseClass)
    val combineAllOptionImpl = SemigroupMethods.combineAllOption(caseClass)

    new Monoid[T]:
      override def empty: T = emptyImpl()
      override def combine(x: T, y: T): T = combineImpl(x, y)
      override def combineN(a: T, n: Int): T = combineNImpl(a, n)
      override def combineAll(as: IterableOnce[T]): T = combineAllImpl(as)
      override def combineAllOption(as: IterableOnce[T]): Option[T] = combineAllOptionImpl(as)
  }

  inline given apply[T](using Mirror.Of[T]): Monoid[T] = derived[T]
end MonoidDerivation

private object MonoidMethods {
  def empty[T, Typeclass[T] <: Monoid[T]](caseClass: CaseClass[Typeclass, T]): () => T =
    new Function0[T] with Serializable:
      @transient private lazy val value = caseClass.construct(_.typeclass.empty)
      override def apply(): T = value

  def combineN[T, Typeclass[T] <: Monoid[T]](caseClass: CaseClass[Typeclass, T]): (T, Int) => T = {
    val emptyImpl = empty(caseClass)
    val f = SemigroupMethods.combineNBase(caseClass)
    (a: T, n: Int) =>
      if (n < 0) {
        throw new IllegalArgumentException("Repeated combining for monoids must have n >= 0")
      } else if (n == 0) {
        emptyImpl()
      } else {
        f(a, n)
      }
  }

  def combineAll[T, Typeclass[T] <: Monoid[T]](
    caseClass: CaseClass[Typeclass, T]
  ): IterableOnce[T] => T = {
    val combineImpl = SemigroupMethods.combine(caseClass)
    val emptyImpl = MonoidMethods.empty(caseClass)
    {
      case it: Iterable[T] if it.nonEmpty =>
        // input is re-iterable and non-empty, combineAll on each field
        val result = Array.fill[Any](caseClass.params.length)(null)
        var i = 0
        while (i < caseClass.params.length) {
          val p = caseClass.params(i)
          result(i) = p.typeclass.combineAll(it.iterator.map(p.deref))
          i += 1
        }
        caseClass.rawConstruct(unsafeWrapArray(result))
      case xs => xs.iterator.foldLeft(emptyImpl())(combineImpl)
    }
  }
}

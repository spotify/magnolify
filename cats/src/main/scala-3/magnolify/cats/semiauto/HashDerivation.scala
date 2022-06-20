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

import cats.Hash
import magnolia1.*
import magnolify.shims.MurmurHash3Compat
import scala.util.hashing.MurmurHash3
import scala.deriving.Mirror

object HashDerivation extends Derivation[Hash] {

  def join[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = {
    val eqvImpl = EqMethods.join(caseClass)

    new Hash[T]:
      override def hash(x: T): Int =
        if (caseClass.params.isEmpty) {
          caseClass.typeInfo.short.hashCode
        } else {
          val seed = MurmurHash3Compat.seed(caseClass.typeInfo.short.hashCode)
          val h = caseClass.params
            .map(p => p.typeclass.hash(p.deref(x)))
            .foldLeft(seed)(MurmurHash3.mix)
          MurmurHash3.finalizeHash(h, caseClass.params.size)
        }

      override def eqv(x: T, y: T): Boolean = eqvImpl(x, y)
  }

  def split[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = {
    val eqvImpl = EqMethods.split(sealedTrait)

    new Hash[T]:
      override def hash(x: T): Int = sealedTrait.choose(x)(sub => sub.typeclass.hash(sub.value))
      override def eqv(x: T, y: T): Boolean = eqvImpl(x, y)
  }

  inline given apply[T](using Mirror.Of[T]): Hash[T] = derived[T]
}

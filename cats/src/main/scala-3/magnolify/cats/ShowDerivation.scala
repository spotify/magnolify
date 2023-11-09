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

package magnolify.cats

import cats.Show
import magnolia1.*

import scala.deriving.Mirror

object ShowDerivation extends Derivation[Show]:

  def join[T](caseClass: CaseClass[Show, T]): Show[T] = new Show[T]:
    override def show(x: T): String = caseClass.params
      .map(p => s"${p.label} = ${p.typeclass.show(p.deref(x))}")
      .mkString(s"${caseClass.typeInfo.full} {", ", ", "}")

  def split[T](sealedTrait: SealedTrait[Show, T]): Show[T] = new Show[T]:
    override def show(x: T): String = sealedTrait.choose(x)(sub => sub.typeclass.show(sub.value))

  inline def gen[T](using Mirror.Of[T]): Show[T] = derivedMirror[T]

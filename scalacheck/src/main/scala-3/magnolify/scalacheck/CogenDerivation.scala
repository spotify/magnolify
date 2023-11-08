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

package magnolify.scalacheck

import magnolia1.*
import org.scalacheck.Cogen

import scala.deriving.Mirror

object CogenDerivation extends Derivation[Cogen]:

  def join[T](caseClass: CaseClass[Cogen, T]): Cogen[T] = Cogen[T] { (seed, t) =>
    caseClass.params.foldLeft(seed) { (s, p) =>
      // inject index to distinguish cases like `(Some(false), None)` and `(None, Some(0))`
      p.typeclass.perturb(Cogen.perturb(s, p.index), p.deref(t))
    }
  }

  def split[T](sealedTrait: SealedTrait[Cogen, T]): Cogen[T] = Cogen[T] { (seed, t) =>
    sealedTrait.choose(t) { sub =>
      // inject index to distinguish case objects instances
      sub.typeclass.perturb(Cogen.perturb(seed, sub.subtype.index), sub.cast(t))
    }
  }

  inline def gen[T](using Mirror.Of[T]): Cogen[T] = derivedMirror[T]

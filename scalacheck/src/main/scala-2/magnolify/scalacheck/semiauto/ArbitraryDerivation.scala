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

package magnolify.scalacheck.semiauto

import magnolia1._
import magnolify.scalacheck.Fallback
import org.scalacheck.{Arbitrary, Gen}


object ArbitraryDerivation {
  type Typeclass[T] = Arbitrary[T]

  def join[T: Fallback](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Arbitrary {
    Gen.lzy(Gen.sized { size =>
      if (size >= 0) {
        Gen.resize(size - 1, caseClass.constructMonadic(_.typeclass.arbitrary)(monadicGen))
      } else {
        implicitly[Fallback[T]].get
      }
    })
  }

  def split[T: Fallback](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = Arbitrary {
    Gen.sized { size =>
      if (size > 0) {
        Gen.resize(
          size - 1,
          Gen.oneOf(sealedTrait.subtypes.map(_.typeclass.arbitrary)).flatMap(identity)
        )
      } else {
        implicitly[Fallback[T]].get
      }
    }
  }

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]

  private val monadicGen: Monadic[Gen] = new Monadic[Gen] {
    override def point[A](value: A): Gen[A] = Gen.const(value)
    override def map[A, B](from: Gen[A])(fn: A => B): Gen[B] = from.map(fn)
    override def flatMap[A, B](from: Gen[A])(fn: A => Gen[B]): Gen[B] = from.flatMap(fn)
  }
}

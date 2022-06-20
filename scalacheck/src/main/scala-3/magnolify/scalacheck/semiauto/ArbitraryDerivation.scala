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

package magnolify.scalacheck.semiauto

import magnolia1.*
import magnolify.scalacheck.Fallback
import org.scalacheck.{Arbitrary, Gen}

import scala.deriving.Mirror

object ArbitraryDerivation extends Derivation[Arbitrary]:

  given Monadic[Gen] with
    def point[A](value: A): Gen[A] = Gen.const(value)
    def map[A, B](from: Gen[A])(fn: A => B): Gen[B] = from.map(fn)
    def flatMap[A, B](from: Gen[A])(fn: A => Gen[B]): Gen[B] = from.flatMap(fn)

  def join[T](caseClass: CaseClass[Arbitrary, T]): Arbitrary[T] = Arbitrary {
    Gen.lzy(Gen.sized { size =>
      if (size >= 0) {
        Gen.resize(size - 1, caseClass.constructMonadic(_.typeclass.arbitrary))
      } else {
        // TODO fallback
        Gen.fail
      }
    })
  }

  def split[T](sealedTrait: SealedTrait[Arbitrary, T]): Arbitrary[T] = Arbitrary {
    Gen.sized { size =>
      if (size > 0) {
        Gen.resize(
          size - 1,
          Gen.oneOf(sealedTrait.subtypes.map(_.typeclass.arbitrary)).flatMap(identity)
        )
      } else {
        // TODO fallback
        Gen.fail
      }
    }
  }

  inline given apply[T](using Mirror.Of[T]): Arbitrary[T] = derived[T]

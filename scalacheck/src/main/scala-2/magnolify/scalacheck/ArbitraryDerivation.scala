/*
 * Copyright 2024 Spotify AB
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
import org.scalacheck.{Arbitrary, Gen}

object ArbitraryDerivation {
  type Typeclass[T] = Arbitrary[T]

  private implicit val monadicGen: Monadic[Gen] = new Monadic[Gen] {
    override def point[A](value: A): Gen[A] = Gen.const(value)
    override def map[A, B](from: Gen[A])(fn: A => B): Gen[B] = from.map(fn)
    override def flatMap[A, B](from: Gen[A])(fn: A => Gen[B]): Gen[B] = from.flatMap(fn)
  }

  def join[T](caseClass: CaseClass[Arbitrary, T]): Arbitrary[T] = Arbitrary {
    caseClass.constructMonadic(_.typeclass.arbitrary)
  }

  def split[T](sealedTrait: SealedTrait[Arbitrary, T]): Arbitrary[T] = Arbitrary {
    Gen.lzy {
      Gen.sized { size =>
        val subtypes = sealedTrait.subtypes
        for {
          i <-
            if (size >= 0) {
              // pick any subtype
              Gen.choose(0, subtypes.size - 1)
            } else {
              // pick a fixed subtype to have a chance to stop recursion
              Gen.const(subtypes.size + size)
            }
          subtypeGen <- Gen.resize(size - 1, sealedTrait.subtypes(i).typeclass.arbitrary)
        } yield subtypeGen
      }
    }
  }

  implicit def gen[T]: Arbitrary[T] = macro Magnolia.gen[T]

  @deprecated("Use gen instead", "0.7.0")
  def apply[T]: Arbitrary[T] = macro Magnolia.gen[T]
}

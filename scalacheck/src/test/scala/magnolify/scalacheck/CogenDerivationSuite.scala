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

package magnolify.scalacheck

import magnolify.test.*
import magnolify.test.ADT.*
import magnolify.test.Simple.*
import org.scalacheck.*
import org.scalacheck.rng.Seed

import java.net.URI
import scala.reflect.*

class CogenDerivationSuite extends MagnolifySuite with magnolify.scalacheck.AutoDerivations {

  import TestArbitrary.arbSeed

  private def test[T: ClassTag](implicit arb: Arbitrary[T], t: Cogen[T]): Unit = {
    // TODO val co = ensureSerializable(t)
    val co = t
    val name = className[T]
    implicit val arbList: Arbitrary[List[T]] = Arbitrary(Gen.listOfN(10, arb.arbitrary))
    property(s"$name.uniqueness") {
      Prop.forAll { (seed: Seed, xs: List[T]) =>
        val coper = xs.map(co.perturb(seed, _))
        val result = coper.toSet.size == xs.toSet.size
        if (!result) {
          println("coper: " + coper)
          println("origin: " + xs)
        }
        result
      }
    }
    property(s"$name.consistency") {
      Prop.forAll { (seed: Seed, x: T) =>
        co.perturb(seed, x) == co.perturb(seed, x)
      }
    }
  }

  import magnolify.scalacheck.TestArbitrary.*
  implicit val cogenUri: Cogen[URI] = Cogen(_.hashCode().toLong)

  test[Numbers]
  test[Required]
  test[Nullable]

  test[Repeated]
  test[Collections]
  test[Nested]
  test[Custom]

  // magnolia scala3 limitation:
  // For a recursive structures it is required to assign the derived value to an implicit variable
  implicit val cogenNode: Cogen[Node] = genCogen
  implicit val cogenGNode: Cogen[GNode[Int]] = genCogen

  test[Node]
  test[GNode[Int]]
  test[Shape]
  test[Color]
}

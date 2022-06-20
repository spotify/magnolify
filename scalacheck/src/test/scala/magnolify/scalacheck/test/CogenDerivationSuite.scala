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

package magnolify.scalacheck.test

import magnolify.scalacheck.semiauto._
import magnolify.test.ADT._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._
import org.scalacheck.rng.Seed

import scala.reflect._

class CogenDerivationSuite extends MagnolifySuite with magnolify.scalacheck.AutoDerivation {

  private def test[T: Arbitrary: ClassTag: Cogen]: Unit =
    test[T, T](identity)

  private def test[T: ClassTag, U](f: T => U)(implicit arb: Arbitrary[T], t: Cogen[T]): Unit = {
    // val co = ensureSerializable(t)
    val co = t
    val name = className[T]
    implicit val arbList: Arbitrary[List[T]] = Arbitrary(Gen.listOfN(10, arb.arbitrary))
    property(s"$name.uniqueness") {
      Prop.forAll { (l: Long, xs: List[T]) =>
        val seed = Seed(l) // prevent Magnolia from deriving `Seed`
        xs.map(co.perturb(seed, _)).toSet.size == xs.map(f).toSet.size
      }
    }
    property(s"$name.consistency") {
      Prop.forAll { (l: Long, x: T) =>
        val seed = Seed(l) // prevent Magnolia from deriving `Seed`
        co.perturb(seed, x) == co.perturb(seed, x)
      }
    }
  }

  test[Numbers]
  test[Required]
  test[Nullable]

  test[Repeated]
  test((c: Collections) => (c.a.toList, c.l, c.v))
  test[Nested]

  import Custom._
  test[Custom]

  // recursive structures require to assign the derived value to an implicit variable
  import magnolify.scalacheck.test.ADT.{arbGNode, arbNode}
  implicit lazy val cogenNode: Cogen[Node] = CogenDerivation[Node]
  implicit lazy val cogenGNode: Cogen[GNode[Int]] = CogenDerivation[GNode[Int]]
  test[Node]
  test[GNode[Int]]

  test[Shape]
  test[Color]
}

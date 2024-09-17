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

import magnolify.scalacheck.MoreCollectionsBuildable._ // extra scala 2.12 Buildable
import magnolify.scalacheck.TestArbitrary.arbDuration
import magnolify.scalacheck.TestArbitrary.arbUri
import magnolify.shims._
import magnolify.test.ADT._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._
import org.scalacheck.rng.Seed

import scala.reflect._

class ArbitraryDerivationSuite extends MagnolifySuite {
  import TestArbitrary.arbSeed
  import magnolify.scalacheck.auto.autoDerivationArbitrary

  private def test[T: ClassTag](implicit t: Arbitrary[T]): Unit = {
    // TODO val g = ensureSerializable(t).arbitrary
    val g = t.arbitrary
    val name = className[T]
    val prms = Gen.Parameters.default
    // `forAll(Gen.listOfN(10, g))` fails for `Repeated` & `Collections` when size parameter <= 1
    property(s"$name.uniqueness") {
      Prop.forAll { (seed: Seed) =>
        Gen.listOfN(10, g)(prms, seed).get.toSet.size > 1
      }
    }
    property(s"$name.consistency") {
      Prop.forAll { (l: Long) =>
        val seed = Seed(l) // prevent Magnolia from deriving `Seed`
        g(prms, seed).get == g(prms, seed).get
      }
    }
  }

  test[Numbers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Collections]
  test[MoreCollections]
  test[Nested]

  {
    implicit val arbInt: Arbitrary[Int] = Arbitrary(Gen.chooseNum(0, 100))
    implicit val arbLong: Arbitrary[Long] = Arbitrary(Gen.chooseNum(100, 10000))
    property("implicits") {
      Prop.forAll { (x: Integers) =>
        x.i >= 0 && x.i <= 100 && x.l >= 100 && x.l <= 10000
      }
    }
  }

  test[Custom]

  // magnolia scala3 limitation:
  // For a recursive structures it is required to assign the derived value to an implicit variable
  import magnolify.scalacheck.semiauto.semiautoDerivationArbitrary
  implicit val arbNode: Arbitrary[Node] = Arbitrary.gen
  implicit val arbGNode: Arbitrary[GNode[Int]] = Arbitrary.gen

  test[Node]
  test[GNode[Int]]
  test[Shape]
  test[Color]
}

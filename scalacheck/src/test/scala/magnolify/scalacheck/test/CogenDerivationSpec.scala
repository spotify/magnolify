/*
 * Copyright 2019 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.scalacheck.test

import magnolify.scalacheck.auto._
import magnolify.test.ADT._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._
import org.scalacheck.rng.Seed

import scala.reflect._

object CogenDerivationSpec extends MagnolifySpec("CogenDerivation") {
  private def test[T: ClassTag](implicit arb: Arbitrary[T], co: Cogen[T]): Unit =
    test[T, T](identity)

  private def test[T: ClassTag, U](f: T => U)(implicit arb: Arbitrary[T], co: Cogen[T]): Unit = {
    ensureSerializable(co)
    val name = className[T]
    implicit val arbList: Arbitrary[List[T]] = Arbitrary(Gen.listOfN(10, arb.arbitrary))
    property(s"$name.uniqueness") = Prop.forAll { (seed: Seed, xs: List[T]) =>
      xs.map(co.perturb(seed, _)).toSet.size == xs.map(f).toSet.size
    }
    property(s"$name.consistency") = Prop.forAll { (seed: Seed, x: T) =>
      co.perturb(seed, x) == co.perturb(seed, x)
    }
  }

  test[Numbers]
  test[Required]

  {
    // FIXME: uniqueness workaround for Nones
    implicit def arbOption[T](implicit arb: Arbitrary[T]): Arbitrary[Option[T]] =
      Arbitrary(Gen.frequency(1 -> Gen.const(None), 99 -> Gen.some(arb.arbitrary)))
    test[Nullable]
  }

  {
    // FIXME: uniqueness workaround for Nils
    implicit def arbList[T](implicit arb: Arbitrary[T]): Arbitrary[List[T]] =
      Arbitrary(Gen.nonEmptyListOf(arb.arbitrary))
    test[Repeated]
    test((c: Collections) => (c.a.toList, c.l, c.v))
  }

  test[Nested]

  import Custom._
  test[Custom]

  test[Node]
  test[GNode[Int]]
  test[Shape]
  test[Color]
}

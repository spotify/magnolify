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

import magnolify.test.Simple._
import magnolify.test.ADT._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class FunctionDerivationSuite extends MagnolifySuite with magnolify.scalacheck.AutoDerivations {
  private def test[A: ClassTag, B: ClassTag](implicit
    t: Arbitrary[A => B],
    arbA: Arbitrary[A]
  ): Unit = {
    // TODO val gf = ensureSerializable(t).arbitrary
    val gf = t.arbitrary
    val ga = arbA.arbitrary
    val name = s"${className[A]}.${className[B]}"
    property(s"$name.consistency") {
      Prop.forAll(gf, ga)((f, a) => f(a) == f(a))
    }
    def genList[T](g: Gen[T]): Gen[List[T]] = Gen.listOfN(100, g)
    property(s"$name.functions") {
      Prop.forAll(genList(gf), ga)((fs, a) => fs.map(_(a)).toSet.size > 1)
    }
    property(s"$name.inputs") {
      Prop.forAll(gf, genList(ga))((f, as) => as.map(f).toSet.size > 1)
    }
  }

  test[Numbers, Numbers]
  test[Shape, Shape]
  test[Numbers, Shape]
  test[Shape, Numbers]
}

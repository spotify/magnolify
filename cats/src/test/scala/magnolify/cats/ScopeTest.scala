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

import cats._
import cats.kernel.{Band, CommutativeGroup, CommutativeMonoid, CommutativeSemigroup}
import magnolify.test.Simple._
import munit.FunSuite

object ScopeTest {
  case class Sets(s: Set[Int])

  object Auto {
    import magnolify.cats.auto._
    val s: Show[Numbers] = implicitly
    val eq: Eq[Numbers] = implicitly
    val hash: Hash[Numbers] = implicitly
    val sg: Semigroup[Numbers] = implicitly
    val m: Monoid[Numbers] = implicitly
    val csg: CommutativeSemigroup[Numbers] = implicitly
    val cm: CommutativeMonoid[Numbers] = implicitly
    val g: Group[Numbers] = implicitly
    val cg: CommutativeGroup[Numbers] = implicitly
    val b: Band[Sets] = implicitly
  }

  object Semi {
    import magnolify.cats.semiauto._
    EqDerivation[Numbers]
    HashDerivation[Numbers]
    SemigroupDerivation[Numbers]
    CommutativeSemigroupDerivation[Numbers]
    BandDerivation[Sets]
    MonoidDerivation[Numbers]
    CommutativeMonoidDerivation[Numbers]
    GroupDerivation[Numbers]
    CommutativeGroupDerivation[Numbers]
    ShowDerivation[Numbers]
  }
}

class ScopeTest extends FunSuite {

  test("auto implicit will give most powerful abstraction") {
    assertEquals(ScopeTest.Auto.s.getClass.getName, "cats.Show$$anon$2")
    assertEquals(
      ScopeTest.Auto.eq.getClass.getName,
      "magnolify.cats.semiauto.HashDerivation$$anon$1"
    )
    assertEquals(
      ScopeTest.Auto.hash.getClass.getName,
      "magnolify.cats.semiauto.HashDerivation$$anon$1"
    )
    assertEquals(
      ScopeTest.Auto.sg.getClass.getName,
      "magnolify.cats.semiauto.CommutativeGroupDerivation$$anon$1"
    )
    assertEquals(
      ScopeTest.Auto.m.getClass.getName,
      "magnolify.cats.semiauto.CommutativeGroupDerivation$$anon$1"
    )
    assertEquals(
      ScopeTest.Auto.csg.getClass.getName,
      "magnolify.cats.semiauto.CommutativeGroupDerivation$$anon$1"
    )
    assertEquals(
      ScopeTest.Auto.cm.getClass.getName,
      "magnolify.cats.semiauto.CommutativeGroupDerivation$$anon$1"
    )
    assertEquals(
      ScopeTest.Auto.g.getClass.getName,
      "magnolify.cats.semiauto.CommutativeGroupDerivation$$anon$1"
    )
    assertEquals(
      ScopeTest.Auto.cg.getClass.getName,
      "magnolify.cats.semiauto.CommutativeGroupDerivation$$anon$1"
    )
    assertEquals(
      ScopeTest.Auto.b.getClass.getName,
      "magnolify.cats.semiauto.BandDerivation$$anon$1"
    )
  }

}

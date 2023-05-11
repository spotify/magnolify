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
import magnolify.cats.semiauto._
import munit.FunSuite

import scala.reflect.{classTag, ClassTag}

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

  def checkImpl[T: ClassTag](tc: Any): Unit = {
    val expected = classTag[T].runtimeClass.getName
    val actual = tc.getClass.getName
    assert(actual.startsWith(expected))
  }

  test("auto implicit will give most powerful abstraction") {
    checkImpl[ShowDerivation.type](ScopeTest.Auto.s)
    checkImpl[HashDerivation.type](ScopeTest.Auto.eq)
    checkImpl[HashDerivation.type](ScopeTest.Auto.hash)
    checkImpl[CommutativeGroupDerivation.type](ScopeTest.Auto.sg)
    checkImpl[CommutativeGroupDerivation.type](ScopeTest.Auto.m)
    checkImpl[CommutativeGroupDerivation.type](ScopeTest.Auto.csg)
    checkImpl[CommutativeGroupDerivation.type](ScopeTest.Auto.cm)
    checkImpl[CommutativeGroupDerivation.type](ScopeTest.Auto.g)
    checkImpl[CommutativeGroupDerivation.type](ScopeTest.Auto.cg)
    checkImpl[BandDerivation.type](ScopeTest.Auto.b)
  }

}

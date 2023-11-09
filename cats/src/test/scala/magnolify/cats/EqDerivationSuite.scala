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

import cats.Eq._
import cats._
import cats.kernel.laws.discipline._
import magnolify.cats.auto.genEq
import magnolify.cats.TestEq.eqArray
import magnolify.cats.TestEq.eqDuration
import magnolify.cats.TestEq.eqUri
import magnolify.scalacheck.TestArbitrary._
import magnolify.scalacheck.TestCogen._
import magnolify.test.ADT._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class EqDerivationSuite extends MagnolifySuite with magnolify.scalacheck.AutoDerivations {
  private def test[T: Arbitrary: ClassTag: Cogen: Eq]: Unit = {
    // TODO val eq = ensureSerializable(implicitly[Eq[T]])
    val eq = Eq[T]
    include(EqTests[T](eq).eqv.all, className[T] + ".")
  }

  test[Numbers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Collections]
  test[Custom]

  // magnolia scala3 limitation:
  // For a recursive structures it is required to assign the derived value to an implicit variable
  // TODO use different implicit names in auto/semiauto to avoid shadowing
  implicit val eqNode: Eq[Node] = magnolify.cats.EqDerivation.gen
  implicit val eqGNode: Eq[GNode[Int]] = magnolify.cats.EqDerivation.gen
  test[Node]
  test[GNode[Int]]

  test[Shape]
  test[Color]
}

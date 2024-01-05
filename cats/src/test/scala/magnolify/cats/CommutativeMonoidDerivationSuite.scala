/*
 * Copyright 2020 Spotify AB
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

import cats.*
import cats.kernel.CommutativeMonoid
import cats.kernel.laws.discipline.*
import magnolify.cats.Types.MiniInt
import magnolify.cats.semiauto.*
import magnolify.test.*
import org.scalacheck.*

import scala.reflect.*

class CommutativeMonoidDerivationSuite extends MagnolifySuite {
  import CommutativeMonoidDerivationSuite.*
  import magnolify.scalacheck.auto.genArbitrary
  import magnolify.cats.auto.genCommutativeMonoid

  private def test[T: Arbitrary: ClassTag: Eq: CommutativeMonoid]: Unit = {
    // TODO val cm = ensureSerializable(implicitly[CommutativeMonoid[T]])
    val cm = CommutativeMonoid[T]
    include(CommutativeMonoidTests[T](cm).commutativeMonoid.all, className[T] + ".")
  }

  implicit val eqRecord: Eq[Record] = Eq.gen[Record]
  implicit val cmMiniInt: CommutativeMonoid[MiniInt] =
    CommutativeMonoid.instance(MiniInt(0), (x, y) => MiniInt(x.i + y.i))

  test[Record]
}

object CommutativeMonoidDerivationSuite {
  case class Record(i: Int, m: MiniInt)
}

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
import cats.kernel.CommutativeSemigroup
import cats.kernel.laws.discipline.*
import magnolify.cats.Types.MiniInt
import magnolify.cats.semiauto.*
import magnolify.test.*
import org.scalacheck.*

import scala.reflect.*

class CommutativeSemigroupDerivationSuite extends MagnolifySuite {
  import CommutativeSemigroupDerivationSuite.*
  import magnolify.scalacheck.auto.*
  import magnolify.cats.auto.autoDerivationCommutativeSemigroup

  private def test[T: Arbitrary: ClassTag: Eq: CommutativeSemigroup]: Unit = {
    // TODO val csg = ensureSerializable(implicitly[CommutativeSemigroup[T]])
    val csg = CommutativeSemigroup[T]
    include(CommutativeSemigroupTests[T](csg).commutativeSemigroup.all, className[T] + ".")
  }

  implicit val eqRecord: Eq[Record] = Eq.gen[Record]
  implicit val csgMiniInt: CommutativeSemigroup[MiniInt] =
    CommutativeSemigroup.instance((x, y) => MiniInt(x.i + y.i))
  test[Record]
}

object CommutativeSemigroupDerivationSuite {
  case class Record(i: Int, m: MiniInt)
}

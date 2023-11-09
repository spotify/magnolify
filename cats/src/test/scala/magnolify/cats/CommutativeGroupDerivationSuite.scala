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
import cats.kernel.CommutativeGroup
import cats.kernel.laws.discipline.*
import magnolify.cats.Types.MiniInt
import magnolify.cats.semiauto.*
import magnolify.test.*
import org.scalacheck.*

import scala.reflect.*

class CommutativeGroupDerivationSuite
    extends MagnolifySuite
    with magnolify.scalacheck.AutoDerivations {
  import CommutativeGroupDerivationSuite.*
  import magnolify.cats.auto.genCommutativeGroup

  private def test[T: Arbitrary: ClassTag: Eq: CommutativeGroup]: Unit = {
    // TODO val cg = ensureSerializable(implicitly[CommutativeGroup[T]])
    val cg = CommutativeGroup[T]
    include(CommutativeGroupTests[T](cg).commutativeGroup.all, className[T] + ".")
  }

  implicit val eqRecord: Eq[Record] = Eq.gen[Record]
  implicit val cgMiniInt: CommutativeGroup[MiniInt] = new CommutativeGroup[MiniInt] {
    override def empty: MiniInt = MiniInt(0)
    override def combine(x: MiniInt, y: MiniInt): MiniInt = MiniInt(x.i + y.i)
    override def inverse(a: MiniInt): MiniInt = MiniInt(-a.i)
  }
  test[Record]
}

object CommutativeGroupDerivationSuite {
  case class Record(i: Int, m: MiniInt)
}

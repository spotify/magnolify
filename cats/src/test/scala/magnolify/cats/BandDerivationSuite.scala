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
import cats.kernel.Band
import cats.kernel.laws.discipline.*
import magnolify.cats.Types.MiniSet
import magnolify.cats.semiauto.*
import magnolify.test.*
import org.scalacheck.*

import scala.reflect.*

class BandDerivationSuite extends MagnolifySuite {
  import BandDerivationSuite.*
  import magnolify.scalacheck.auto.*
  import magnolify.cats.auto.autoDerivationBand

  private def test[T: Arbitrary: ClassTag: Eq: Band]: Unit = {
    // TODO val band = ensureSerializable(implicitly[Band[T]])
    val band = Band[T]
    include(BandTests[T](band).band.all, className[T] + ".")
  }

  implicit val eqRecord: Eq[Record] = Eq.gen[Record]
  implicit val bMiniSet: Band[MiniSet] = Band.instance((x, y) => MiniSet(x.s ++ y.s))

  test[Record]
}

object BandDerivationSuite {
  case class Record(s: Set[Int], m: MiniSet)
}

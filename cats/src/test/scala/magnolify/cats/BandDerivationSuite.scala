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

import cats._
import cats.kernel.Band
import cats.kernel.laws.discipline._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class BandDerivationSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag: Eq: Band]: Unit = {
    val band = ensureSerializable(implicitly[Band[T]])
    include(BandTests[T](band).band.all, className[T] + ".")
  }

  import BandDerivationSuite._
  test[Record]
}

object BandDerivationSuite {
  import Types.MiniSet
  implicit val bMiniSet: Band[MiniSet] = Band.instance((x, y) => MiniSet(x.s ++ y.s))
  case class Record(s: Set[Int], m: MiniSet)
}

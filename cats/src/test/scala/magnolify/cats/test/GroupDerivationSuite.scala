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

package magnolify.cats.test

import cats._
import cats.kernel.laws.discipline._
import magnolify.test._
import magnolify.scalacheck.semiauto.ArbitraryDerivation
import magnolify.cats.test.Types.MiniInt
import org.scalacheck._

import scala.reflect._

class GroupDerivationSuite extends MagnolifySuite with magnolify.cats.AutoDerivation {

  private def test[T: Arbitrary: ClassTag: Eq: Group]: Unit = {
    // val grp = ensureSerializable(implicitly[Group[T]])
    val grp = implicitly[Group[T]]
    include(GroupTests[T](grp).group.all, className[T] + ".")
  }

  {
    import cats.Eq._
    import GroupDerivationSuite._
    implicit val arbRecord: Arbitrary[Record] = ArbitraryDerivation[Record]
    implicit val gMiniInt: Group[MiniInt] = new Group[MiniInt] {
      override def empty: MiniInt = MiniInt(0)
      override def combine(x: MiniInt, y: MiniInt): MiniInt = MiniInt(x.i + y.i)
      override def inverse(a: MiniInt): MiniInt = MiniInt(-a.i)
    }
    test[Record]
  }
}

object GroupDerivationSuite {
  case class Record(i: Int, m: MiniInt)
}

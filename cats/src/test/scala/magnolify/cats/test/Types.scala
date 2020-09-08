/*
 * Copyright 2019 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.cats.test

import cats.Eq
import org.scalacheck.Arbitrary

object Types {
  class MiniInt(val i: Int) extends Serializable

  object MiniInt {
    def apply(i: Int): MiniInt = new MiniInt(i)

    implicit val arbMiniInt: Arbitrary[MiniInt] = Arbitrary(
      Arbitrary.arbInt.arbitrary.map(MiniInt(_))
    )
    implicit val eqMiniInt: Eq[MiniInt] = Eq.by(_.i)
  }

  class MiniSet(val s: Set[Int])

  object MiniSet {
    def apply(s: Set[Int]): MiniSet = new MiniSet(s)

    implicit val arbMiniSet: Arbitrary[MiniSet] = Arbitrary(
      Arbitrary.arbContainer[Set, Int].arbitrary.map(MiniSet(_))
    )
    implicit val eqMiniSet: Eq[MiniSet] = Eq.by(_.s)
  }

}

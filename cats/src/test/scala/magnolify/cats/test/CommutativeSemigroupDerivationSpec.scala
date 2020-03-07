/*
 * Copyright 2020 Spotify AB.
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

import cats._
import cats.instances.all._
import cats.kernel.CommutativeSemigroup
import cats.kernel.laws.discipline._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object CommutativeSemigroupDerivationSpec extends MagnolifySpec("CommutativeSemigroupDerivation") {
  private def test[T: Arbitrary: ClassTag: Eq: CommutativeSemigroup]: Unit = {
    ensureSerializable(implicitly[CommutativeSemigroup[T]])
    include(CommutativeSemigroupTests[T].commutativeSemigroup.all, className[T] + ".")
  }

  import Types.MiniInt
  implicit val csgMiniInt: CommutativeSemigroup[MiniInt] = new CommutativeSemigroup[MiniInt] {
    override def combine(x: MiniInt, y: MiniInt): MiniInt = MiniInt(x.i + y.i)
  }
  case class Record(i: Int, m: MiniInt)
  test[Record]
}

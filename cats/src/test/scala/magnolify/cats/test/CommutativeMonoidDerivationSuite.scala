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
import cats.kernel.CommutativeMonoid
import cats.kernel.laws.discipline._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

class CommutativeMonoidDerivationSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag: Eq: CommutativeMonoid]: Unit = {
    val cm = ensureSerializable(implicitly[CommutativeMonoid[T]])
    include(CommutativeMonoidTests[T](cm).commutativeMonoid.all, className[T] + ".")
  }

  import CommutativeMonoidDerivationSuite._
  test[Record]
}

object CommutativeMonoidDerivationSuite {
  import Types.MiniInt
  implicit val cmMiniInt: CommutativeMonoid[MiniInt] =
    CommutativeMonoid.instance(MiniInt(0), (x, y) => MiniInt(x.i + y.i))
  case class Record(i: Int, m: MiniInt)
}

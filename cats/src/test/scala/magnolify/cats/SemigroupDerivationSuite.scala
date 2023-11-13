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

import cats.*
import cats.kernel.laws.discipline.*
import magnolify.cats.Types.MiniInt
import magnolify.cats.semiauto.*
import magnolify.test.*
import magnolify.test.Simple.*
import org.scalacheck.*

import java.net.URI
import java.time.Duration
import scala.reflect.*

class SemigroupDerivationSuite extends MagnolifySuite {
  import SemigroupDerivationSuite.*
  import magnolify.cats.auto.genSemigroup
  import magnolify.scalacheck.auto.genArbitrary

  private def test[T: Arbitrary: ClassTag: Eq: Semigroup]: Unit = {
    // TODO val sg = ensureSerializable(implicitly[Semigroup[T]])
    val sg = Semigroup[T]
    include(SemigroupTests[T](sg).semigroup.all, className[T] + ".")
  }

  import magnolify.scalacheck.TestArbitrary.*
  import magnolify.cats.TestEq.*
  implicit val eqRecord: Eq[Record] = Eq.gen[Record]
  implicit val sgBool: Semigroup[Boolean] = Semigroup.instance(_ ^ _)
  implicit val sgUri: Semigroup[URI] =
    Semigroup.instance((x, y) => URI.create(x.toString + y.toString))
  implicit val sgDuration: Semigroup[Duration] = Semigroup.instance(_ plus _)
  implicit val sgMiniInt: Semigroup[MiniInt] = Semigroup.instance((x, y) => MiniInt(x.i + y.i))

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Custom]
  test[Record]
}

object SemigroupDerivationSuite {
  case class Record(i: Int, m: MiniInt)
}

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
import magnolify.cats.TestEq.*
import magnolify.cats.Types.MiniInt
import magnolify.cats.semiauto.*
import magnolify.test.*
import magnolify.test.Simple.*
import org.scalacheck.*

import java.net.URI
import java.time.Duration
import scala.reflect.*

class MonoidDerivationSuite extends MagnolifySuite {
  import MonoidDerivationSuite.*
  import magnolify.scalacheck.auto.genArbitrary
  import magnolify.cats.auto.genMonoid

  private def test[T: Arbitrary: ClassTag: Eq: Monoid]: Unit = {
    // TODO val mon = ensureSerializable(implicitly[Monoid[T]])
    val mon = Monoid[T]
    include(MonoidTests[T](mon).monoid.all, className[T] + ".")
  }

  import magnolify.scalacheck.TestArbitrary.*
  implicit val eqRecord: Eq[Record] = Eq.gen[Record]
  implicit val mBool: Monoid[Boolean] = Monoid.instance(false, _ || _)
  implicit val mUri: Monoid[URI] =
    Monoid.instance(URI.create(""), (x, y) => URI.create(x.toString + y.toString))
  implicit val mDuration: Monoid[Duration] =
    Monoid.instance(Duration.ZERO, _ plus _)
  implicit val mMiniInt: Monoid[MiniInt] =
    Monoid.instance(MiniInt(0), (x, y) => MiniInt(x.i + y.i))

  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Custom]

  test[Record]
}

object MonoidDerivationSuite {
  case class Record(i: Int, m: MiniInt)
}

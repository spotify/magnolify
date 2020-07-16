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

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object SemigroupDerivationSpec extends MagnolifySpec("SemigroupDerivation") {
  private def test[T: Arbitrary: ClassTag: Eq: Semigroup]: Unit = {
    val sg = ensureSerializable(implicitly[Semigroup[T]])
    include(SemigroupTests[T](sg).semigroup.all, className[T] + ".")
  }

  import Types.MiniInt
  implicit val sgMiniInt: Semigroup[MiniInt] = Semigroup.instance((x, y) => MiniInt(x.i + y.i))
  case class Record(i: Int, m: MiniInt)
  test[Record]

  test[Integers]

  {
    implicit val sgBool: Semigroup[Boolean] = Semigroup.instance(_ ^ _)
    test[Required]
    test[Nullable]
    test[Repeated]
    test[Nested]
  }

  {
    import Custom._
    implicit val sgUri: Semigroup[URI] =
      Semigroup.instance((x, y) => URI.create(x.toString + y.toString))
    implicit val sgDuration: Semigroup[Duration] = Semigroup.instance(_ plus _)
    test[Custom]
  }
}

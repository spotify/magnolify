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

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.ADT._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object HashDerivationSpec extends MagnolifySpec("HashDerivation") {
  private def test[T: Arbitrary: ClassTag: Cogen: Hash]: Unit = test()

  private def test[T: Arbitrary: ClassTag: Cogen: Hash](exclusions: String*): Unit = {
    val hash = ensureSerializable(implicitly[Hash[T]])
    val props = HashTests[T](hash).hash.props.filter(kv => !exclusions.contains(kv._1))
    for ((n, p) <- props) {
      property(s"${className[T]}.$n") = p
    }
  }

  // Long.## != Long.hashCode
  test[Integers]("same as universal hash")
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    implicit val hash: Hash[Array[Int]] = Hash.by(_.toList)
    test[Collections]("same as scala hashing", "same as universal hash")
  }

  {
    import Custom._
    test[Custom]
  }

  test[Node]
  test[GNode[Int]]
  test[Shape]
  test[Color]
}

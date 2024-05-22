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
import magnolify.test.*
import magnolify.test.ADT.*
import magnolify.test.Simple.*
import org.scalacheck.*

import java.net.URI
import java.time.Duration
import scala.reflect.*

class HashDerivationSuite extends MagnolifySuite {
  import magnolify.cats.auto.genHash

  private def test[T: Arbitrary: ClassTag: Cogen: Hash](exclusions: String*): Unit = {
    // TODO val hash = ensureSerializable(implicitly[Hash[T]])
    val hash = Hash[T]
    val props = HashTests[T](hash).hash.props.filter(kv => !exclusions.contains(kv._1))
    for ((n, p) <- props) {
      property(s"${className[T]}.$n")(p)
    }
  }

  import magnolify.scalacheck.TestArbitrary.*
  import magnolify.scalacheck.TestCogen.*
  // Use `scala.util.hashing.Hashing[T]` for `Array[Int]`, equivalent to `x.##` and `x.hashCode`
  implicit val hash: Hash[Array[Int]] = Hash.fromHashing[Array[Int]]
  implicit val hashUri: Hash[URI] = Hash.fromUniversalHashCode
  implicit val hashDuration: Hash[Duration] = Hash.fromUniversalHashCode

  // Long.## != Long.hashCode for negative values
  test[Integers]("same as scala hashing", "same as universal hash")
  test[Required]()
  test[Nullable]()
  test[Repeated]()
  test[Nested]()
  test[Collections]()
  test[Custom]()

  // magnolia scala3 limitation:
  // For a recursive structures it is required to assign the derived value to an implicit variable
  // TODO use different implicit names in auto/semiauto to avoid shadowing
  implicit val hashNode: Hash[Node] = magnolify.cats.HashDerivation.gen
  implicit val hashGNode: Hash[GNode[Int]] = magnolify.cats.HashDerivation.gen
  test[Node]()
  test[GNode[Int]]()

  test[Shape]()
  test[Color]()
}

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
import magnolify.test.ADT._
import magnolify.test.Simple._
import magnolify.test._
import magnolify.cats.semiauto.HashDerivation
import org.scalacheck._

import scala.reflect._
import scala.util.hashing.MurmurHash3

import java.time.Duration
import java.net.URI

class HashDerivationSuite extends MagnolifySuite with magnolify.cats.AutoDerivation {

  private def test[T: Arbitrary: ClassTag: Cogen: Hash]: Unit = test[T]()

  private def test[T: Arbitrary: ClassTag: Cogen: Hash](exclusions: String*): Unit = {
//    val hash = ensureSerializable(implicitly[Hash[T]])
    val hash = implicitly[Hash[T]]
    val props = HashTests[T](hash).hash.props.filter(kv => !exclusions.contains(kv._1))
    for ((n, p) <- props) {
      property(s"${className[T]}.$n")(p)
    }
  }

  {
    import cats.Eq._
    import magnolify.scalacheck.test.TestArbitraryImplicits._
    import magnolify.scalacheck.test.TestCogenImplicits._
    // MurmurHash3.arrayHashing is different from `x.##` and `x.hashCode`
    implicit val hashArray: Hash[Array[Int]] = Hash.fromHashing(MurmurHash3.arrayHashing)
    implicit val hashUri: Hash[URI] = Hash.fromUniversalHashCode
    implicit val hashDuration: Hash[Duration] = Hash.fromUniversalHashCode

    // Long.## != Long.hashCode for negative values
    test[Integers]("same as scala hashing", "same as universal hash")
    test[Required]
    test[Nullable]
    test[Repeated]
    test[Nested]

    test[Collections]("same as scala hashing", "same as universal hash")
    test[Custom]

    // recursive structures require to assign the derived value to an implicit variable
    implicit lazy val hashNode: Hash[Node] = HashDerivation[Node]
    implicit lazy val hashGNode: Hash[GNode[Int]] = HashDerivation[GNode[Int]]
    test[Node]
    test[GNode[Int]]

    test[Shape]
    test[Color]
  }
}

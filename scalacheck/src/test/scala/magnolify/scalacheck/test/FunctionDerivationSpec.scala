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
package magnolify.scalacheck.test

import magnolify.test.Simple._
import magnolify.test.ADT._
import magnolify.scalacheck.auto._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object FunctionDerivationSpec extends MagnolifySpec("FunctionDerivation") {
  private def test[A: Arbitrary: ClassTag, B: ClassTag](implicit arb: Arbitrary[A => B]): Unit = {
    ensureSerializable(arb)
    val name = s"${className[A]}.${className[B]}"
    property(s"$name.consistency") = Prop.forAll((f: A => B, a: A) => f(a) == f(a))
    implicit def arbList[T](implicit arb: Arbitrary[T]): Arbitrary[List[T]] =
      Arbitrary(Gen.listOfN(100, arb.arbitrary))
    property(s"$name.functions") = Prop.forAll { (fs: List[A => B], a: A) =>
      fs.map(_(a)).toSet.size > 1
    }
    property(s"$name.inputs") = Prop.forAll((f: A => B, as: List[A]) => as.map(f).toSet.size > 1)
  }

  test[Numbers, Numbers]

  {
    // Gen[A => B] depends on Gen[B] and may run out of size
    import magnolify.scalacheck.semiauto.ArbitraryDerivation.Fallback
    implicit val f: Fallback[Shape] = Fallback[Circle]
    test[Shape, Shape]
    test[Numbers, Shape]
  }

  test[Shape, Numbers]
}

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

import cats.Show
import cats.laws.discipline.{ContravariantTests, MiniInt}
import cats.laws.discipline.arbitrary.*
import cats.laws.discipline.eq.*
import magnolify.test.*
import magnolify.test.ADT.*
import magnolify.test.Simple.*
import org.scalacheck.*

import java.net.URI
import java.time.Duration
import scala.reflect.*

class ShowDerivationSuite extends MagnolifySuite {
  import magnolify.cats.auto.autoDerivationShow

  private def test[T: Arbitrary: ClassTag: Show]: Unit = {
    // val show = ensureSerializable(implicitly[Show[T]])
    val show = Show[T]
    val name = className[T]
    include(ContravariantTests[Show].contravariant[MiniInt, Int, Boolean].all, s"$name.")

    property(s"$name.fullName") {
      Prop.forAll { (v: T) =>
        val fullName = v.getClass.getCanonicalName.stripSuffix("$")
        val s = show.show(v)
        s.startsWith(s"$fullName {") && s.endsWith("}")
      }
    }
  }

  import magnolify.scalacheck.TestArbitrary.*
  implicit val showArray: Show[Array[Int]] = Show.fromToString
  implicit val showUri: Show[URI] = Show.fromToString
  implicit val showDuration: Show[Duration] = Show.fromToString

  test[Numbers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Collections]
  test[Custom]

  // magnolia scala3 limitation:
  // For a recursive structures it is required to assign the derived value to an implicit variable
  // TODO use different implicit names in auto/semiauto to avoid shadowing
  implicit val showNode: Show[Node] = magnolify.cats.ShowDerivation.gen
  implicit val showGNode: Show[GNode[Int]] = magnolify.cats.ShowDerivation.gen
  test[Node]
  test[GNode[Int]]

  test[Shape]
  test[Color]
}

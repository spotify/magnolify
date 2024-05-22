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

package magnolify.scalacheck

import magnolify.test.Simple.*
import org.scalacheck.*

object ScopeTest {
  object Auto {
    import magnolify.scalacheck.auto.*
    implicitly[Arbitrary[Numbers]]
    implicitly[Cogen[Numbers]]
    implicitly[Arbitrary[Numbers => Numbers]]
  }

  object Semi {
    import magnolify.scalacheck.semiauto.*
    implicit val arb: Arbitrary[Numbers] = Arbitrary.gen[Numbers]
    implicit val cogen: Cogen[Numbers] = Cogen.gen[Numbers]
    // T => T is not a case class, so ArbitraryDerivation.apply won't work
    implicitly[Arbitrary[Numbers => Numbers]]
  }
}

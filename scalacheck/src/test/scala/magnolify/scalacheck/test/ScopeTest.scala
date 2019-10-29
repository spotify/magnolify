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
import org.scalacheck._

object ScopeTest {
  object Auto {
    import magnolify.scalacheck.auto._
    implicitly[Arbitrary[Numbers]]
    implicitly[Cogen[Numbers]]
    implicitly[Arbitrary[Numbers => Numbers]]
  }

  object Semi {
    import magnolify.scalacheck.semiauto._
    implicit val arb: Arbitrary[Numbers] = ArbitraryDerivation[Numbers]
    implicit val cogen: Cogen[Numbers] = CogenDerivation[Numbers]
    // T => T is not a case class, so ArbitraryDerivation.apply won't work
    implicitly[Arbitrary[Numbers => Numbers]]
  }
}

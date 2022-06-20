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

import org.scalacheck._
import magnolify.scalacheck.auto.ScalacheckMacros
import org.scalacheck.rng.Seed

package object auto extends AutoDerivation

trait AutoDerivation {
  implicit val seedArbitrary: Arbitrary[Seed] = Arbitrary(
    Arbitrary.arbLong.arbitrary.map(Seed.apply)
  )

  implicit def genArbitrary[T]: Arbitrary[T] = macro ScalacheckMacros.genArbitraryMacro[T]
  implicit def genCogen[T]: Cogen[T] = macro ScalacheckMacros.genCogenMacro[T]
}

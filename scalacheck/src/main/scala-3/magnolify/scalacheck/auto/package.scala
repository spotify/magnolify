/*
 * Copyright 2022 Spotify AB
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

import magnolify.scalacheck.semiauto.{ArbitraryDerivation, CogenDerivation}
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Cogen, Gen}

import scala.deriving.Mirror

package object auto extends AutoDerivation

trait AutoDerivation: // extends FallbackDerivation:
  given arbSeed: Arbitrary[Seed] = Arbitrary(Arbitrary.arbLong.arbitrary.map(Seed.apply))

  inline given autoDerivedArbitrary[T](using Mirror.Of[T]): Arbitrary[T] = ArbitraryDerivation[T]
  inline given autoDerivedCogen[T](using Mirror.Of[T]): Cogen[T] = CogenDerivation[T]
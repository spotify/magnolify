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
import cats.kernel.*

import scala.deriving.Mirror

// set implicit priority to avoid conflicts
// see: https://typelevel.org/cats/guidelines.html#implicit-instance-priority
// use shapeless.LowPriority so the
// provided cats type classes are always preferred
// triggers derivation as last resort
trait AutoDerivation extends LowPriority0Implicits

trait LowPriority0Implicits extends LowPriority1Implicits:
  inline given genShow[T](using Mirror.Of[T]): Show[T] = ShowDerivation.gen[T]
  // CommutativeGroup <: Group | CommutativeMonoid
  inline given genCommutativeGroup[T](using Mirror.Of[T]): CommutativeGroup[T] =
    CommutativeGroupDerivation.gen[T]
  // Hash <: Eq
  inline given genHash[T](using Mirror.Of[T]): Hash[T] = HashDerivation.gen[T]

trait LowPriority1Implicits extends LowPriority2Implicits:
  inline given genEq[T](using Mirror.Of[T]): Eq[T] = EqDerivation.gen[T]
  // Group <: Monoid
  inline given genGroup[T](using Mirror.Of[T]): Group[T] = GroupDerivation.gen[T]

trait LowPriority2Implicits extends LowPriority3Implicits:
  // CommutativeMonoid <: Monoid | CommutativeSemigroup
  inline given genCommutativeMonoid[T](using Mirror.Of[T]): CommutativeMonoid[T] =
    CommutativeMonoidDerivation.gen[T]

trait LowPriority3Implicits extends LowPriority4Implicits:
  // CommutativeSemigroup <: Semigroup
  inline given genCommutativeSemigroup[T](using Mirror.Of[T]): CommutativeSemigroup[T] =
    CommutativeSemigroupDerivation.gen[T]
  // Monoid <: Semigroup
  inline given genMonoid[T](using Mirror.Of[T]): Monoid[T] = MonoidDerivation.gen[T]

trait LowPriority4Implicits extends LowPriority5Implicits:
  // Band <: Semigroup
  inline given genBand[T](using Mirror.Of[T]): Band[T] = BandDerivation.gen[T]

trait LowPriority5Implicits:
  inline given genSemigroup[T](using Mirror.Of[T]): Semigroup[T] = SemigroupDerivation.gen[T]

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

package magnolify.cats

import cats.*
import cats.kernel.{Band, CommutativeGroup, CommutativeMonoid, CommutativeSemigroup}
import cats.kernel.instances.*
import magnolify.cats.semiauto.*

import scala.deriving.Mirror

package object auto extends AutoDerivation

// auto derived type classes need to be prioritized
// from the more generic to specific
trait AutoDerivation extends Priority0AutoDerivation

trait Priority0AutoDerivation extends Priority1AutoDerivation:
  inline given autoDerivedEq[T](using Mirror.Of[T]): Eq[T] = EqDerivation[T]
  inline given autoDerivedSemigroup[T](using Mirror.Of[T]): Semigroup[T] = SemigroupDerivation[T]
  inline given autoDerivedShow[T](using Mirror.Of[T]): Show[T] = ShowDerivation[T]

trait Priority1AutoDerivation extends Priority2AutoDerivation:
  // Hash <: Eq
  inline given autoDerivedHash[T](using Mirror.Of[T]): Hash[T] = HashDerivation[T]
  // CommutativeSemigroup <: Semigroup
  inline given autoDerivedCommutativeSemigroup[T](using Mirror.Of[T]): CommutativeSemigroup[T] =
    CommutativeSemigroupDerivation[T]
trait Priority2AutoDerivation extends Priority3AutoDerivation:
  // Band <: Semigroup
  inline given autoDerivedBand[T](using Mirror.Of[T]): Band[T] = BandDerivation[T]

trait Priority3AutoDerivation extends Priority4AutoDerivation:
  // Monoid <: Semigroup
  inline given autoDerivedMonoid[T](using Mirror.Of[T]): Monoid[T] = MonoidDerivation[T]

trait Priority4AutoDerivation extends Priority5AutoDerivation:
  // CommutativeMonoid <: Monoid | CommutativeSemigroup
  inline given autoDerivedCommutativeMonoid[T](using Mirror.Of[T]): CommutativeMonoid[T] =
    CommutativeMonoidDerivation[T]

trait Priority5AutoDerivation extends Priority6AutoDerivation:
  // Group <: Monoid
  inline given autoDerivedGroup[T](using Mirror.Of[T]): Group[T] = GroupDerivation[T]

trait Priority6AutoDerivation:
  // CommutativeGroup <: Group | CommutativeMonoid
  inline given autoDerivedCommutativeGroup[T](using Mirror.Of[T]): CommutativeGroup[T] =
    CommutativeGroupDerivation[T]

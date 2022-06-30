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

import cats._
import cats.kernel.{Band, CommutativeGroup, CommutativeMonoid, CommutativeSemigroup}
import magnolify.cats.auto.CatsMacros

package object auto extends AutoDerivation

trait AutoDerivation {
  // Hash <: Eq
  implicit def derivedHash[T](implicit lp: shapeless.LowPriority): Hash[T] =
    macro CatsMacros.genHashMacro[T]
  implicit def derivedEq[T](implicit lp: shapeless.LowPriority): Eq[T] =
    macro CatsMacros.genEqMacro[T]
  implicit def derivedShow[T](implicit lp: shapeless.LowPriority): Show[T] =
    macro CatsMacros.genShowMacro[T]

  // CommutativeGroup <: Group | CommutativeMonoid
  implicit def derivedCommutativeGroup[T](implicit lp: shapeless.LowPriority): CommutativeGroup[T] =
    macro CatsMacros.genCommutativeGroupMacro[T]

  // Group <: Monoid
  implicit def derivedGroup[T](implicit lp: shapeless.LowPriority): Group[T] =
    macro CatsMacros.genGroupMacro[T]

  // CommutativeMonoid <: Monoid | CommutativeSemigroup
  implicit def derivedCommutativeMonoid[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeMonoid[T] =
    macro CatsMacros.genCommutativeMonoidMacro[T]

  // Monoid <: Semigroup
  implicit def derivedMonoid[T](implicit lp: shapeless.LowPriority): Monoid[T] =
    macro CatsMacros.genMonoidMacro[T]

  // Band <: Semigroup
  implicit def derivedBand[T](implicit lp: shapeless.LowPriority): Band[T] =
    macro CatsMacros.genBandMacro[T]

  // CommutativeSemigroup <: Semigroup
  implicit def derivedCommutativeSemigroup[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeSemigroup[T] =
    macro CatsMacros.genCommutativeSemigroupMacro[T]

  implicit def derivedSemigroup[T](implicit lp: shapeless.LowPriority): Semigroup[T] =
    macro CatsMacros.genSemigroupMacro[T]
}

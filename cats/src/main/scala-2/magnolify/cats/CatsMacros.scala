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

import scala.annotation.nowarn
import scala.reflect.macros.*

private object CatsMacros {

  @nowarn("msg=parameter lp in method autoDerivationShowMacro is never used")
  def autoDerivationShowMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.ShowDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationEqMacro is never used")
  def autoDerivationEqMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.EqDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationHashMacro is never used")
  def autoDerivationHashMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.HashDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationSemigroupMacro is never used")
  def autoDerivationSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.SemigroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationMonoidMacro is never used")
  def autoDerivationMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.MonoidDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationCommutativeSemigroupMacro is never used")
  def autoDerivationCommutativeSemigroupMacro[T: c.WeakTypeTag](
    c: whitebox.Context
  )(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeSemigroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationCommutativeMonoidMacro is never used")
  def autoDerivationCommutativeMonoidMacro[T: c.WeakTypeTag](
    c: whitebox.Context
  )(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeMonoidDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationGroupMacro is never used")
  def autoDerivationGroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.GroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationCommutativeGroupMacro is never used")
  def autoDerivationCommutativeGroupMacro[T: c.WeakTypeTag](
    c: whitebox.Context
  )(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeGroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationBandMacro is never used")
  def autoDerivationBandMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.BandDerivation.gen[$wtt]"""
  }
}

// set implicit priority to avoid conflicts
// see: https://typelevel.org/cats/guidelines.html#implicit-instance-priority
// use shapeless.LowPriority so the
// provided cats type classes are always preferred
// triggers derivation as last resort
trait AutoDerivation extends LowPriority0Implicits

trait LowPriority0Implicits extends LowPriority1Implicits {
  implicit def autoDerivationShow[T](implicit lp: shapeless.LowPriority): Show[T] =
    macro CatsMacros.autoDerivationShowMacro[T]
  // CommutativeGroup <: Group | CommutativeMonoid
  implicit def autoDerivationCommutativeGroup[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeGroup[T] =
    macro CatsMacros.autoDerivationCommutativeGroupMacro[T]
  // Hash <: Eq
  implicit def autoDerivationHash[T](implicit lp: shapeless.LowPriority): Hash[T] =
    macro CatsMacros.autoDerivationHashMacro[T]
}

trait LowPriority1Implicits extends LowPriority2Implicits {
  implicit def autoDerivationEq[T](implicit lp: shapeless.LowPriority): Eq[T] =
    macro CatsMacros.autoDerivationEqMacro[T]
  // Group <: Monoid
  implicit def autoDerivationGroup[T](implicit lp: shapeless.LowPriority): Group[T] =
    macro CatsMacros.autoDerivationGroupMacro[T]
}

trait LowPriority2Implicits extends LowPriority3Implicits {
  // CommutativeMonoid <: Monoid | CommutativeSemigroup
  implicit def autoDerivationCommutativeMonoid[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeMonoid[T] =
    macro CatsMacros.autoDerivationCommutativeMonoidMacro[T]
}

trait LowPriority3Implicits extends LowPriority4Implicits {
  // CommutativeSemigroup <: Semigroup
  implicit def autoDerivationCommutativeSemigroup[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeSemigroup[T] =
    macro CatsMacros.autoDerivationCommutativeSemigroupMacro[T]
  // Monoid <: Semigroup
  implicit def autoDerivationMonoid[T](implicit lp: shapeless.LowPriority): Monoid[T] =
    macro CatsMacros.autoDerivationMonoidMacro[T]
}

trait LowPriority4Implicits extends LowPriority5Implicits {
  // Band <: Semigroup
  implicit def autoDerivationBand[T](implicit lp: shapeless.LowPriority): Band[T] =
    macro CatsMacros.autoDerivationBandMacro[T]
}

trait LowPriority5Implicits {
  implicit def autoDerivationSemigroup[T](implicit lp: shapeless.LowPriority): Semigroup[T] =
    macro CatsMacros.autoDerivationSemigroupMacro[T]
}

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

  @nowarn("msg=parameter lp in method genShowMacro is never used")
  def genShowMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.ShowDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genEqMacro is never used")
  def genEqMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.EqDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genHashMacro is never used")
  def genHashMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.HashDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genSemigroupMacro is never used")
  def genSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.SemigroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genMonoidMacro is never used")
  def genMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.MonoidDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genCommutativeSemigroupMacro is never used")
  def genCommutativeSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeSemigroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genCommutativeMonoidMacro is never used")
  def genCommutativeMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeMonoidDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genGroupMacro is never used")
  def genGroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.GroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genCommutativeGroupMacro is never used")
  def genCommutativeGroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeGroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method genBandMacro is never used")
  def genBandMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
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
  implicit def genShow[T](implicit lp: shapeless.LowPriority): Show[T] =
    macro CatsMacros.genShowMacro[T]
  // CommutativeGroup <: Group | CommutativeMonoid
  implicit def genCommutativeGroup[T](implicit lp: shapeless.LowPriority): CommutativeGroup[T] =
    macro CatsMacros.genCommutativeGroupMacro[T]
  // Hash <: Eq
  implicit def genHash[T](implicit lp: shapeless.LowPriority): Hash[T] =
    macro CatsMacros.genHashMacro[T]
}

trait LowPriority1Implicits extends LowPriority2Implicits {
  implicit def genEq[T](implicit lp: shapeless.LowPriority): Eq[T] =
    macro CatsMacros.genEqMacro[T]
  // Group <: Monoid
  implicit def genGroup[T](implicit lp: shapeless.LowPriority): Group[T] =
    macro CatsMacros.genGroupMacro[T]
}

trait LowPriority2Implicits extends LowPriority3Implicits {
  // CommutativeMonoid <: Monoid | CommutativeSemigroup
  implicit def genCommutativeMonoid[T](implicit lp: shapeless.LowPriority): CommutativeMonoid[T] =
    macro CatsMacros.genCommutativeMonoidMacro[T]
}

trait LowPriority3Implicits extends LowPriority4Implicits {
  // CommutativeSemigroup <: Semigroup
  implicit def genCommutativeSemigroup[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeSemigroup[T] =
    macro CatsMacros.genCommutativeSemigroupMacro[T]
  // Monoid <: Semigroup
  implicit def genMonoid[T](implicit lp: shapeless.LowPriority): Monoid[T] =
    macro CatsMacros.genMonoidMacro[T]
}

trait LowPriority4Implicits extends LowPriority5Implicits {
  // Band <: Semigroup
  implicit def genBand[T](implicit lp: shapeless.LowPriority): Band[T] =
    macro CatsMacros.genBandMacro[T]
}

trait LowPriority5Implicits {
  implicit def genSemigroup[T](implicit lp: shapeless.LowPriority): Semigroup[T] =
    macro CatsMacros.genSemigroupMacro[T]
}

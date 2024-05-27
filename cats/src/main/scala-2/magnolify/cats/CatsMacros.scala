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

  @nowarn("msg=parameter lp in method autoDerivationShow is never used")
  def autoDerivationShow[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.ShowDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationEq is never used")
  def autoDerivationEq[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.EqDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationHash is never used")
  def autoDerivationHash[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.HashDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationSemigroup is never used")
  def autoDerivationSemigroup[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.SemigroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationMonoid is never used")
  def autoDerivationMonoid[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.MonoidDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationCommutativeSemigroup is never used")
  def autoDerivationCommutativeSemigroup[T: c.WeakTypeTag](
    c: whitebox.Context
  )(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeSemigroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationCommutativeMonoid is never used")
  def autoDerivationCommutativeMonoid[T: c.WeakTypeTag](
    c: whitebox.Context
  )(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeMonoidDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationGroup is never used")
  def autoDerivationGroup[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.GroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationCommutativeGroup is never used")
  def autoDerivationCommutativeGroup[T: c.WeakTypeTag](
    c: whitebox.Context
  )(lp: c.Tree): c.Tree = {
    import c.universe.*
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.CommutativeGroupDerivation.gen[$wtt]"""
  }

  @nowarn("msg=parameter lp in method autoDerivationBand is never used")
  def autoDerivationBand[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
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
    macro CatsMacros.autoDerivationShow[T]
  // CommutativeGroup <: Group | CommutativeMonoid
  implicit def autoDerivationCommutativeGroup[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeGroup[T] =
    macro CatsMacros.autoDerivationCommutativeGroup[T]
  // Hash <: Eq
  implicit def autoDerivationHash[T](implicit lp: shapeless.LowPriority): Hash[T] =
    macro CatsMacros.autoDerivationHash[T]
}

trait LowPriority1Implicits extends LowPriority2Implicits {
  implicit def autoDerivationEq[T](implicit lp: shapeless.LowPriority): Eq[T] =
    macro CatsMacros.autoDerivationEq[T]
  // Group <: Monoid
  implicit def autoDerivationGroup[T](implicit lp: shapeless.LowPriority): Group[T] =
    macro CatsMacros.autoDerivationGroup[T]
}

trait LowPriority2Implicits extends LowPriority3Implicits {
  // CommutativeMonoid <: Monoid | CommutativeSemigroup
  implicit def autoDerivationCommutativeMonoid[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeMonoid[T] =
    macro CatsMacros.autoDerivationCommutativeMonoid[T]
}

trait LowPriority3Implicits extends LowPriority4Implicits {
  // CommutativeSemigroup <: Semigroup
  implicit def autoDerivationCommutativeSemigroup[T](implicit
    lp: shapeless.LowPriority
  ): CommutativeSemigroup[T] =
    macro CatsMacros.autoDerivationCommutativeSemigroup[T]
  // Monoid <: Semigroup
  implicit def autoDerivationMonoid[T](implicit lp: shapeless.LowPriority): Monoid[T] =
    macro CatsMacros.autoDerivationMonoid[T]
}

trait LowPriority4Implicits extends LowPriority5Implicits {
  // Band <: Semigroup
  implicit def autoDerivationBand[T](implicit lp: shapeless.LowPriority): Band[T] =
    macro CatsMacros.autoDerivationBand[T]
}

trait LowPriority5Implicits {
  implicit def autoDerivationSemigroup[T](implicit lp: shapeless.LowPriority): Semigroup[T] =
    macro CatsMacros.autoDerivationSemigroup[T]
}

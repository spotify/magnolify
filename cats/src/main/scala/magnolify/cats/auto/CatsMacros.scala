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
package magnolify.cats.auto

import _root_.cats._
import cats.kernel.{Band, CommutativeMonoid, CommutativeSemigroup}
import cats.kernel.instances._

import scala.language.experimental.macros
import scala.reflect.macros._

private object CatsMacros {
  def genEqMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.EqDerivation.apply[$wtt]"""
  }

  def genHashMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.HashDerivation.apply[$wtt]"""
  }

  def genSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.SemigroupDerivation.apply[$wtt]"""
  }

  def genCommutativeSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.CommutativeSemigroupDerivation.apply[$wtt]"""
  }

  def genBandMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.BandDerivation.apply[$wtt]"""
  }

  def genMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.MonoidDerivation.apply[$wtt]"""
  }

  def genCommutativeMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.CommutativeMonoidDerivation.apply[$wtt]"""
  }

  def genGroupMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.GroupDerivation.apply[$wtt]"""
  }

  def genShowMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.ShowDerivation.apply[$wtt]"""
  }
}

trait LowPriorityImplicits extends LowPriorityGenGroup
  with LowPriorityGenHash with LowPriorityGenShow {
  // more specific implicits to workaround ambiguous implicit values with cats
  implicit def genListMonoid[T] = new ListMonoid[T]
  implicit def genOptionMonoid[T: Semigroup] = new OptionMonoid[T]
  implicit def genListOrder[T: Order] = new ListOrder[T]
  implicit def genOptionOrder[T: Order] = new OptionOrder[T]
}

trait LowPriorityGenGroup extends LowPriorityGenMonoid {
  implicit def genGroup[T]: Group[T] = macro CatsMacros.genGroupMacro[T]
}

trait LowPriorityGenMonoid extends LowPriorityGenSemigroup {
  implicit def genCommutativeMonoid[T]: CommutativeMonoid[T] = macro CatsMacros.genCommutativeMonoidMacro[T]
  implicit def genMonoid[T]: Monoid[T] = macro CatsMacros.genMonoidMacro[T]
}

trait LowPriorityGenSemigroup {
  implicit def genCommutativeSemigroup[T]: CommutativeSemigroup[T] = macro CatsMacros.genCommutativeSemigroupMacro[T]
  implicit def genBand[T]: Band[T] = macro CatsMacros.genBandMacro[T]
  implicit def genSemigroup[T]: Semigroup[T] = macro CatsMacros.genSemigroupMacro[T]
}

trait LowPriorityGenHash {
  // more specific implicits to workaround ambiguous implicit values with cats
  implicit def genListHash[T: Hash] = new ListHash[T]
  implicit def genOptionHash[T: Hash] = new OptionHash[T]

  implicit def genHash[T]: Hash[T] = macro CatsMacros.genHashMacro[T]
  implicit def genEq[T]: Eq[T] = macro CatsMacros.genEqMacro[T]
}

trait LowPriorityGenShow {
  implicit def genShow[T]: Show[T] = macro CatsMacros.genShowMacro[T]
}

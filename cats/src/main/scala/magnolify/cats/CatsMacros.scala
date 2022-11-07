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

import scala.reflect.macros._
import scala.annotation.nowarn

private object CatsMacros {
  @nowarn("msg=parameter value lp in method genEqMacro is never used")
  def genEqMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.EqDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genHashMacro is never used")
  def genHashMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.HashDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genSemigroupMacro is never used")
  def genSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.SemigroupDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genCommutativeSemigroupMacro is never used")
  def genCommutativeSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.CommutativeSemigroupDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genBandMacro is never used")
  def genBandMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.BandDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genMonoidMacro is never used")
  def genMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.MonoidDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genCommutativeMonoidMacro is never used")
  def genCommutativeMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.CommutativeMonoidDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genGroupMacro is never used")
  def genGroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.GroupDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genCommutativeGroupMacro is never used")
  def genCommutativeGroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.CommutativeGroupDerivation.apply[$wtt]"""
  }

  @nowarn("msg=parameter value lp in method genShowMacro is never used")
  def genShowMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.ShowDerivation.apply[$wtt]"""
  }
}

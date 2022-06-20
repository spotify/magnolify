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

package magnolify.cats.auto

import scala.annotation.nowarn
import scala.reflect.macros._

object CatsMacros {
  @nowarn
  def genEqMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.EqDerivation.apply[$wtt]"""
  }

  @nowarn
  def genHashMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.HashDerivation.apply[$wtt]"""
  }

  @nowarn
  def genSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.SemigroupDerivation.apply[$wtt]"""
  }

  @nowarn
  def genCommutativeSemigroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.CommutativeSemigroupDerivation.apply[$wtt]"""
  }

  @nowarn
  def genBandMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.BandDerivation.apply[$wtt]"""
  }

  @nowarn
  def genMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.MonoidDerivation.apply[$wtt]"""
  }

  @nowarn
  def genCommutativeMonoidMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.CommutativeMonoidDerivation.apply[$wtt]"""
  }

  @nowarn
  def genGroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.GroupDerivation.apply[$wtt]"""
  }

  @nowarn
  def genCommutativeGroupMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.CommutativeGroupDerivation.apply[$wtt]"""
  }

  @nowarn
  def genShowMacro[T: c.WeakTypeTag](c: whitebox.Context)(lp: c.Tree): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.cats.semiauto.ShowDerivation.apply[$wtt]"""
  }
}

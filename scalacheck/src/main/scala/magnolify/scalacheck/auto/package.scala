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

package magnolify.scalacheck

import org.scalacheck._

import scala.reflect.macros._

package object auto {
  def genArbitraryMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.scalacheck.semiauto.ArbitraryDerivation.apply[$wtt]"""
  }

  def genCogenMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.scalacheck.semiauto.CogenDerivation.apply[$wtt]"""
  }

  implicit def genArbitrary[T]: Arbitrary[T] = macro genArbitraryMacro[T]
  implicit def genCogen[T]: Cogen[T] = macro genCogenMacro[T]
}

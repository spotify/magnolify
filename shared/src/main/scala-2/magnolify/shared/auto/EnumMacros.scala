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

package magnolify.shared.auto

import scala.reflect.macros._

object EnumMacros {
  def genEnumTypeMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    q"""_root_.magnolify.shared.semiauto.EnumTypeDerivation.apply[$wtt]"""
  }

  def scalaEnumTypeMacro[T: c.WeakTypeTag](
    c: whitebox.Context
  ) /* (annotations: c.Expr[AnnotationType[T]]) */: c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    val ref = wtt.tpe.asInstanceOf[TypeRef]
    val name = ref.pre.typeSymbol.asClass.fullName // find the enum type from the value type
    val idx = name.lastIndexOf('.')
    val n = name.drop(idx + 1)
    val ns = name.take(idx)
    val list = q"${ref.pre.termSymbol}.values.iterator.map(_.toString).toList"
    val map = q"${ref.pre.termSymbol}.values.iterator.map(x => x.toString -> x).toMap"
    q"""
        _root_.magnolify.shared.EnumType.create[$wtt](
          $n, $ns, $list, List.empty, $map.apply(_)
        )
     """
  }

}
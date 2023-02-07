/*
 * Copyright 2023 Spotify AB
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

package magnolify.shared

import scala.reflect.macros._

object EnumTypeMacros {

  def scalaEnumTypeImpl[T: c.WeakTypeTag](
    c: whitebox.Context
  )(annotations: c.Expr[AnnotationType[T]]): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    val ref = wtt.tpe.asInstanceOf[TypeRef]
    val fn = ref.pre.typeSymbol.asClass.fullName
    val idx = fn.lastIndexOf('.')
    val n = fn.drop(idx + 1) // `object <Namespace> extends Enumeration`
    val ns = fn.take(idx)
    val list = q"${ref.pre.termSymbol}.values.toList.sortBy(_.id).map(_.toString)"
    val map = q"${ref.pre.termSymbol}.values.map(x => x.toString -> x).toMap"

    q"""
        _root_.magnolify.shared.EnumType.create[$wtt](
          $n, $ns, $list, $annotations.annotations, $map.apply(_))
     """
  }

}

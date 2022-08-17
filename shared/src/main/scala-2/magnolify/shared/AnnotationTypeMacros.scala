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

package magnolify.shared

import scala.reflect.macros.whitebox

object AnnotationTypeMacros {
  def annotationTypeMacro[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val wtt = weakTypeTag[T]
    val pre = wtt.tpe.asInstanceOf[TypeRef].pre

    // Scala 2.12 & 2.13 macros seem to handle annotations differently
    // Scala annotation works in both but Java annotations only works in 2.13
    val saType = typeOf[scala.annotation.StaticAnnotation]
    val jaType = typeOf[java.lang.annotation.Annotation]
    // Annotation for Scala enumerations are on the outer object
    val annotated = if (pre <:< typeOf[scala.Enumeration]) pre else wtt.tpe
    val trees = annotated.typeSymbol.annotations.collect {
      case t if t.tree.tpe <:< saType && !(t.tree.tpe <:< jaType) =>
        // FIXME `t.tree` should work but somehow crashes the compiler
        val q"new $n(..$args)" = t.tree
        q"new $n(..$args)"
    }

    // Get Java annotations via reflection
    val j = q"classOf[${annotated.typeSymbol.asClass}].getAnnotations.toList"
    val annotations = q"_root_.scala.List(..$trees) ++ $j"

    q"_root_.magnolify.shared.AnnotationType[$wtt]($annotations)"
  }
}

trait AnnotationTypeCompanionMacros {
  implicit def gen[T]: AnnotationType[T] = macro AnnotationTypeMacros.annotationTypeMacro[T]
}

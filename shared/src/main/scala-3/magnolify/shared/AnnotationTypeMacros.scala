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

import scala.quoted.*
import scala.reflect.ClassTag

object AnnotationTypeMacros:
  private def getClassTag[T](using Type[T], Quotes): Expr[ClassTag[T]] =
    import quotes.reflect._
    Expr.summon[ClassTag[T]] match {
      case Some(ct) =>
        ct
      case None =>
        report.error(
          s"Unable to find a ClassTag for type ${Type.show[T]}",
          Position.ofMacroExpansion
        )
        throw new Exception("Error when applying macro")
    }

  def annotationTypeMacro[T: Type](using quotes: Quotes): Expr[AnnotationType[T]] =
    import quotes.reflect.*
    val annotated = Type.of[T] match {
      case '[Enumeration#Value] =>
        // Annotation for Scala enumerations are on the outer object
        val TypeRef(pre, _) = TypeRepr.of[T]
        pre
      case _ =>
        TypeRepr.of[T]
    }
    // only collect scala annotations
    val sAnnotations =
      Expr.ofList[Any](annotated.typeSymbol.annotations.map(_.asExprOf[Any]).filter {
        case '{ $x: java.lang.annotation.Annotation }   => false
        case '{ $x: scala.annotation.StaticAnnotation } => true
        case _                                          => false
      })
    val jAnnotations = annotated.asType match {
      case '[t] => '{ ${ getClassTag[t] }.runtimeClass.getAnnotations.toList }
    }
    val annotations = '{ $sAnnotations ++ $jAnnotations }
    '{ AnnotationType[T]($annotations) }

trait AnnotationTypeCompanionMacros:
  inline given gen[T]: AnnotationType[T] = ${ AnnotationTypeMacros.annotationTypeMacro[T] }

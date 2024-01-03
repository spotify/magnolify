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

import scala.quoted.*
import scala.deriving.Mirror

object EnumTypeMacros:
  def scalaEnumTypeMacro[T: Type](annotations: Expr[AnnotationType[T]])(using
    quotes: Quotes
  ): Expr[EnumType[T]] =
    import quotes.reflect.*
    val TypeRef(ref, _) = TypeRepr.of[T]: @unchecked // find the enum type from the value type
    val e = Ref(ref.termSymbol).asExprOf[Enumeration]
    val name = ref.show
    val idx = name.lastIndexOf('.')
    val n = Expr(name.drop(idx + 1))
    val ns = Expr(name.take(idx))
    val vs = '{ $e.values.toList.map(_.toString) }
    val as = '{ $annotations.annotations }
    val map = '{ $e.values.iterator.map(x => x.toString -> x.asInstanceOf[T]).toMap.apply(_) }
    '{ EnumType.create[T]($n, $ns, $vs, $as, $map) }

trait EnumTypeCompanionMacros extends EnumTypeCompanionMacros0

trait EnumTypeCompanionMacros0 extends EnumTypeCompanionMacros1:
  inline implicit def scalaEnumType[T <: Enumeration#Value](using
    annotations: AnnotationType[T]
  ): EnumType[T] =
    ${ EnumTypeMacros.scalaEnumTypeMacro[T]('annotations) }

trait EnumTypeCompanionMacros1 extends EnumTypeDerivation:
  inline implicit def gen[T](using Mirror.Of[T]): EnumType[T] = derivedMirror[T]

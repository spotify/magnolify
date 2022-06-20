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

import magnolify.shared.EnumType

import scala.quoted.{Expr, Quotes, Type}

object EnumMacros:

  def scalaEnumTypeMacro[T: Type](using quotes: Quotes): Expr[EnumType[T]] =
    import quotes.reflect.*
    val TypeRef(ref, _) = TypeRepr.of[T] // find the enum type from the value type
    val e = Ref(ref.termSymbol).asExprOf[Enumeration]
    val name = ref.show
    val idx = name.lastIndexOf('.')
    val n = Expr(name.drop(idx + 1))
    val ns = Expr(name.take(idx))
    val list = '{ $e.values.map(_.toString).toList }
    val map = '{ $e.values.iterator.map(x => x.toString -> x.asInstanceOf[T]).toMap.apply(_) }
    '{ EnumType.create[T]($n, $ns, $list, List.empty, $map) }

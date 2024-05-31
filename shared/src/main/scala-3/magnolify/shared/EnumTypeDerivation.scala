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

import magnolia1.*

import scala.compiletime.*
import scala.deriving.Mirror

trait EnumTypeDerivation:
  implicit inline def gen[T](using mirror: Mirror.Of[T]): EnumType[T] = EnumTypeDerivation.gen[T]

// Do not extend Derivation so we can add an extra check when deriving the sum type
object EnumTypeDerivation:

  private transparent inline def values[A, S <: Tuple](m: Mirror.Of[A]): List[A] =
    inline erasedValue[S] match
      case _: EmptyTuple =>
        Nil
      case _: (s *: tail) =>
        val infos = summonFrom {
          case mm: Mirror.SumOf[`s`] =>
            values[A, mm.MirroredElemTypes](mm.asInstanceOf[m.type])
          case mm: Mirror.ProductOf[`s`] if Macro.isObject[`s`] =>
            List(mm.fromProduct(EmptyTuple).asInstanceOf[A])
          case _ =>
            error("Cannot derive EnumType for non singleton sum type")
        }
        infos ::: values[A, tail](m)

  inline implicit def gen[T](using mirror: Mirror.Of[T]): EnumType[T] =
    val s = values[T, T *: EmptyTuple](mirror).sortBy(_.toString)
    val it = Macro.typeInfo[T]
    val annotations = Macro.inheritedAnns[T] ++ Macro.anns[T]

    EnumType.create(
      it.short,
      it.owner,
      s.map(_.toString),
      annotations,
      name => s.find(_.toString == name).get
    )

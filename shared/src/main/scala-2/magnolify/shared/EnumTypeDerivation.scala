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

import magnolia1.{CaseClass, Magnolia, SealedTrait}

import scala.annotation.{implicitNotFound, nowarn}

trait EnumTypeDerivation {
  type Typeclass[T] = EnumType[T]

  // EnumType can only be split into objects with fixed name
  // Avoid invalid ADT derivation involving products by requiring
  // implicit EnumValue type-class in magnolia join
  // see https://github.com/softwaremill/magnolia/issues/267
  @implicitNotFound("Cannot derive EnumType.EnumValue. EnumType only works for sum types")
  trait EnumValue[T]

  implicit def genEnumValue[T]: EnumValue[T] = macro EnumTypeMacros.genEnumValueMacro[T]

  @nowarn
  def join[T: EnumValue](caseClass: CaseClass[EnumType, T]): EnumType[T] = {
    val n = caseClass.typeName.short
    val ns = caseClass.typeName.owner
    EnumType.create(
      n,
      ns,
      List(n),
      caseClass.annotations.toList,
      _ => caseClass.rawConstruct(Nil)
    )
  }

  def split[T](sealedTrait: SealedTrait[EnumType, T]): EnumType[T] = {
    val n = sealedTrait.typeName.short
    val ns = sealedTrait.typeName.owner
    val subs = sealedTrait.subtypes.map(_.typeclass)
    val values = subs.flatMap(_.values).sorted.toList
    val annotations = (sealedTrait.annotations ++ subs.flatMap(_.annotations)).toList
    EnumType.create(
      n,
      ns,
      values,
      annotations,
      // it is ok to use the inefficient find here because it will be called only once
      // and cached inside an instance of EnumType
      v => subs.find(_.name == v).get.from(v)
    )
  }

  implicit def gen[T]: EnumType[T] = macro Magnolia.gen[T]
}

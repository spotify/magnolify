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

import magnolia1.{CommonDerivation, SealedTraitDerivation, CaseClass, SealedTrait}

import scala.compiletime.*
import scala.deriving.Mirror
trait EnumTypeDerivation extends CommonDerivation[EnumType] with SealedTraitDerivation:
  transparent inline def subtypes[T, SubtypeTuple <: Tuple](
    m: Mirror.SumOf[T],
    idx: Int = 0 // no longer used, kept for bincompat
  ): List[SealedTrait.Subtype[Typeclass, T, _]] =
    subtypesFromMirror[T, SubtypeTuple](m, idx)

  inline def derivedMirrorSum[A](sum: Mirror.SumOf[A]): EnumType[A] =
    summonAll[Tuple.Map[sum.MirroredElemTypes, [S] =>> S <:< Singleton]] // assert all singleton
    split(sealedTraitFromMirror(sum))

  inline def derivedMirror[A](using mirror: Mirror.Of[A]): EnumType[A] =
    inline mirror match
      case sum: Mirror.SumOf[A]         => derivedMirrorSum[A](sum)
      case product: Mirror.ProductOf[A] => derivedMirrorProduct[A](product)

  inline def derived[A](using Mirror.Of[A]): EnumType[A] = derivedMirror[A]

  protected override inline def deriveSubtype[s](m: Mirror.Of[s]): EnumType[s] = derivedMirror[s](using m)

  def join[T](caseClass: CaseClass[EnumType, T]): EnumType[T] =
    // fail at runtime since we can't prevent derivation
    // see https://github.com/softwaremill/magnolia/issues/267
    require(caseClass.isObject, s"Cannot derive EnumType[T] for case class ${caseClass.typeInfo}")
    val n = caseClass.typeInfo.short
    val ns = caseClass.typeInfo.owner
    EnumType.create(
      n,
      ns,
      List(n),
      caseClass.annotations.toList,
      _ => caseClass.rawConstruct(Nil)
    )
  end join

  def split[T](sealedTrait: SealedTrait[EnumType, T]): EnumType[T] =
    val n = sealedTrait.typeInfo.short
    val ns = sealedTrait.typeInfo.owner
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
  end split

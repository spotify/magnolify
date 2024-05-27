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

package magnolify.cats

import cats.Show
import cats.kernel.*

package object semiauto {

  @deprecated("Use Band.gen[T] instead", "0.7.0")
  val BandDerivation = magnolify.cats.BandDerivation
  @deprecated("Use CommutativeGroup.gen[T] instead", "0.7.0")
  val CommutativeGroupDerivation = magnolify.cats.CommutativeGroupDerivation
  @deprecated("Use CommutativeMonoid.gen[T] instead", "0.7.0")
  val CommutativeMonoidDerivation = magnolify.cats.CommutativeMonoidDerivation
  @deprecated("Use CommutativeSemigroup.gen[T] instead", "0.7.0")
  val CommutativeSemigroupDerivation = magnolify.cats.CommutativeSemigroupDerivation
  @deprecated("Use Eq.gen[T] instead", "0.7.0")
  val EqDerivation = magnolify.cats.EqDerivation
  @deprecated("Use Group.gen[T] instead", "0.7.0")
  val GroupDerivation = magnolify.cats.GroupDerivation
  @deprecated("Use Hash.gen[T] instead", "0.7.0")
  val HashDerivation = magnolify.cats.HashDerivation
  @deprecated("Use Semigroup.gen[T] instead", "0.7.0")
  val SemigroupDerivation = magnolify.cats.SemigroupDerivation
  @deprecated("Use Show.gen[T] instead", "0.7.0")
  val ShowDerivation = magnolify.cats.ShowDerivation

  implicit def semiautoDerivationBand(b: Band.type): magnolify.cats.BandDerivation.type =
    magnolify.cats.BandDerivation

  implicit def semiautoDerivationCommutativeGroup(
    cg: CommutativeGroup.type
  ): magnolify.cats.CommutativeGroupDerivation.type =
    magnolify.cats.CommutativeGroupDerivation

  implicit def semiautoDerivationCommutativeMonoid(
    cm: CommutativeMonoid.type
  ): magnolify.cats.CommutativeMonoidDerivation.type =
    magnolify.cats.CommutativeMonoidDerivation

  implicit def semiautoDerivationCommutativeSemigroup(
    cm: CommutativeSemigroup.type
  ): magnolify.cats.CommutativeSemigroupDerivation.type =
    magnolify.cats.CommutativeSemigroupDerivation

  implicit def semiautoDerivationEq(eq: Eq.type): magnolify.cats.EqDerivation.type =
    magnolify.cats.EqDerivation

  implicit def semiautoDerivationGroup(g: Group.type): magnolify.cats.GroupDerivation.type =
    magnolify.cats.GroupDerivation

  implicit def semiautoDerivationHash(h: Hash.type): magnolify.cats.HashDerivation.type =
    magnolify.cats.HashDerivation

  implicit def semiautoDerivationMonoid(m: Monoid.type): magnolify.cats.MonoidDerivation.type =
    magnolify.cats.MonoidDerivation

  implicit def semiautoDerivationSemigroup(
    sg: Semigroup.type
  ): magnolify.cats.SemigroupDerivation.type =
    magnolify.cats.SemigroupDerivation

  implicit def semiautoDerivationShow(sg: Show.type): magnolify.cats.ShowDerivation.type =
    magnolify.cats.ShowDerivation

}

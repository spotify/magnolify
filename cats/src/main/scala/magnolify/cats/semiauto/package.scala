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

package object semiauto extends CatsImplicits {

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

}

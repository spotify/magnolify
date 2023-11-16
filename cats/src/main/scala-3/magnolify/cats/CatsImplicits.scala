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

import scala.deriving.Mirror

trait CatsImplicits:

  // format: off
  extension (b: Band.type) inline def gen[T](using Mirror.Of[T]): Band[T] = BandDerivation.gen
  extension (cg: CommutativeGroup.type) inline def gen[T](using Mirror.Of[T]): CommutativeGroup[T] = CommutativeGroupDerivation.gen

  extension (cm: CommutativeMonoid.type) inline def gen[T](using Mirror.Of[T]): CommutativeMonoid[T] = CommutativeMonoidDerivation.gen

  extension (cm: CommutativeSemigroup.type) inline def gen[T](using Mirror.Of[T]): CommutativeSemigroup[T] = CommutativeSemigroupDerivation.gen

  extension (eq: Eq.type) inline def gen[T](using Mirror.Of[T]): Eq[T] = EqDerivation.gen

  extension (g: Group.type) inline def gen[T](using Mirror.Of[T]): Group[T] = GroupDerivation.gen

  extension (h: Hash.type) inline def gen[T](using Mirror.Of[T]): Hash[T] = HashDerivation.gen

  extension (m: Monoid.type) inline def gen[T](using Mirror.Of[T]): Monoid[T] = MonoidDerivation.gen

  extension (sg: Semigroup.type) inline def gen[T](using Mirror.Of[T]): Semigroup[T] = SemigroupDerivation.gen

  extension (s: Show.type) inline def gen[T](using Mirror.Of[T]): Show[T] = ShowDerivation.gen
  // format: on

object CatsImplicits

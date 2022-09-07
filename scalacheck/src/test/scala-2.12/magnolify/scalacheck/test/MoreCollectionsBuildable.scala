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

package magnolify.scalacheck.test

import magnolify.shims._
import org.scalacheck.util.Buildable

import scala.collection.immutable

object MoreCollectionsBuildable {

  import Buildable._

  implicit def buildableIterable[T]: Buildable[T, Iterable[T]] =
    buildableCanBuildFrom(listCBF)
  implicit def buildableImmutableSeq[T]: Buildable[T, immutable.Seq[T]] =
    buildableCanBuildFrom(listCBF)
  implicit def buildableIndexedSeq[T]: Buildable[Int, IndexedSeq[Int]] =
    buildableCanBuildFrom(vectorCBF)

}

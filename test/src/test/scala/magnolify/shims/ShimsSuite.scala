/*
 * Copyright 2019 Spotify AB
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

import magnolify.test._
import org.scalacheck._

import scala.reflect.ClassTag
import scala.collection.compat._
import scala.collection.compat.immutable._

class CollectionSuite extends MagnolifySuite {

  private def test[C[_]](implicit
    ct: ClassTag[C[Int]],
    ti: C[Int] => Iterable[Int],
    fc: Factory[Int, C[Int]]
  ): Unit = {
    property(className[C[Int]]) {
      Prop.forAll { (xs: List[Int]) =>
        val b = fc.newBuilder
        b ++= xs
        ti(b.result()) == xs
      }
    }
  }

  test[Array]
  test[Iterable]
  test[Seq]
  test[IndexedSeq]
  test[List]
  test[Vector]
  test[LazyList]
}

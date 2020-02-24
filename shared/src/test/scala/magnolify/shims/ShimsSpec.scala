/*
 * Copyright 2019 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.shims

import org.scalacheck._

import scala.reflect._

object ShimsSpec extends Properties("Shims") {
  private def test[C[_]](
    implicit ct: ClassTag[C[Int]],
    ti: C[Int] => Iterable[Int],
    fc: FactoryCompat[Int, C[Int]]
  ): Unit = {
    val name = ct.runtimeClass.getSimpleName
    property(name) = Prop.forAll { xs: List[Int] => fc.build(xs).toList == xs }
  }

  test[Array]
  // Deprecated in 2.13
  // test[Traversable]
  test[Iterable]
  test[Seq]
  test[IndexedSeq]
  test[List]
  test[Vector]
  // Deprecated in 2.13
  // test[Stream]
}

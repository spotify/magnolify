/*
 * Copyright 2021 Spotify AB.
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
package magnolify.cats.test

import cats._
import com.twitter.algebird.{Semigroup => _, _}
import magnolify.cats.auto._
import magnolify.test._

import scala.reflect.ClassTag

class AlgebirdSuite extends MagnolifySuite {
  private def test[T: ClassTag](x: T, y: T, expected: T)(implicit sg: Semigroup[T]): Unit =
    test(className[T]) {
      assertEquals(sg.combine(x, y), expected)
    }

  test(Min(0), Min(1), Min(0))
  test(Max(0), Max(1), Max(1))
}

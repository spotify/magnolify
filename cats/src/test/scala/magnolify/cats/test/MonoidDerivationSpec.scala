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
package magnolify.cats.test

import java.net.URI
import java.time.Duration

import cats._
import cats.instances.all._
import cats.kernel.CommutativeMonoid
import cats.kernel.laws.discipline._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

object MonoidDerivationSpec extends MagnolifySpec("MonoidDerivation") {
  private def test[T: Arbitrary: ClassTag: Eq: Monoid]: Unit = {
    ensureSerializable(implicitly[Monoid[T]])
    include(MonoidTests[T].monoid.all, className[T] + ".")
  }

  test[Integers]

  {
    implicit val mBool: Monoid[Boolean] = Monoid.instance(false, _ || _)
    test[Required]
    implicitly[Monoid[Option[Boolean]]] // FIXME: 2.11 workaround for test[Nullable]
    test[Nullable]
    test[Repeated]
    // FIXME: breaks 2.11: magnolia.Deferred is used for derivation of recursive typeclasses
    // test[Nested]
  }

  {
    implicit val eqArray: Eq[Array[Int]] = Eq.by(_.toList)
    implicit val mArray: Monoid[Array[Int]] = Monoid.instance(Array.emptyIntArray, _ ++ _)
    test[Collections]
  }

  {
    import Custom._
    implicit val mUri: Monoid[URI] =
      Monoid.instance(URI.create(""), (x, y) => URI.create(x.toString + y.toString))
    implicit val mDuration: Monoid[Duration] = Monoid.instance(Duration.ZERO, _ plus _)
    test[Custom]
  }
}

object CommutativeMonoidDerivationSpec extends MagnolifySpec("CommutativeMonoidDerivation") {
  private def test[T: Arbitrary : ClassTag : Eq : CommutativeMonoid]: Unit = {
    ensureSerializable(implicitly[CommutativeMonoid[T]])
    include(CommutativeMonoidTests[T].commutativeMonoid.all, className[T] + ".")
  }

  test[Integers]
}

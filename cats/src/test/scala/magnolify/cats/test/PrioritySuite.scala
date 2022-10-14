/*
 * Copyright 2021 Spotify AB
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

package magnolify.cats.test

import cats._
import com.twitter.algebird.{Semigroup => _, _}
import magnolify.cats.auto._
import magnolify.shims.MurmurHash3Compat
import magnolify.test.Simple._
import magnolify.test._

import scala.reflect.ClassTag
import scala.util.hashing.MurmurHash3

class PrioritySuite extends MagnolifySuite {
  private def test[T: ClassTag](x: T, y: T, expected: T)(implicit sg: Semigroup[T]): Unit =
    test(s"Semigroup.${className[T]}") {
      assertEquals(sg.combine(x, y), expected)
    }

  private def test[T: ClassTag: Hash: Show]: Unit =
    test(s"Priority.${className[T]}") {
      ensureSerializable(implicitly[Eq[T]])
      ensureSerializable(implicitly[Hash[T]])
      ensureSerializable(implicitly[Show[T]])
    }

  test(Min(0), Min(1), Min(0))
  test(Max(0), Max(1), Max(1))

  test[Integers]
  test[Floats]
  test[Numbers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    implicit def hashIterable[T, C[_]](implicit ht: Hash[T], tt: C[T] => Iterable[T]): Hash[C[T]] =
      new Hash[C[T]] {
        override def hash(x: C[T]): Int = {
          val seed = MurmurHash3Compat.seed(x.getClass.hashCode())
          val h = x.foldLeft(seed)((h, p) => MurmurHash3.mix(h, ht.hash(p)))
          MurmurHash3.finalizeHash(h, x.size)
        }
        override def eqv(x: C[T], y: C[T]): Boolean =
          x.size == y.size && (x.iterator zip y.iterator).forall((ht.eqv _).tupled)
      }
    implicit def showIterable[T, C[_]](implicit st: Show[T], tt: C[T] => Iterable[T]): Show[C[T]] =
      Show.show(_.map(st.show).mkString("[", ",", "]"))
    test[Collections]
    test[MoreCollections]
  }

  {
    import Enums._
    implicit val hashScalaEnum: Hash[ScalaEnums.Color.Type] = Hash.by(_.toString)
    implicit val hashJavaEnum: Hash[JavaEnums.Color] = Hash.by(_.name())
    implicit val showScalaEnum: Show[ScalaEnums.Color.Type] = Show.show(_.toString)
    implicit val showJavaEnum: Show[JavaEnums.Color] = Show.show(_.toString)
    test[Enums]
  }
}

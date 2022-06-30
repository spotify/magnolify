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
import magnolify.cats.semiauto._
import magnolify.test.Simple._
import magnolify.test._
import magnolify.shims._

import scala.reflect.ClassTag
import scala.util.hashing.MurmurHash3

class PrioritySuite extends MagnolifySuite with magnolify.cats.AutoDerivation {

  private def test[T: ClassTag: Eq: Hash: Show]: Unit =
    test(s"Priority.${className[T]}") {
      //      ensureSerializable(implicitly[Eq[T]])
      //      ensureSerializable(implicitly[Hash[T])
      //      ensureSerializable(implicitly[Show[T]])
      implicitly[Eq[T]]
      implicitly[Hash[T]]
      implicitly[Show[T]]
    }

  test[Integers]
  test[Floats]
  test[Numbers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    implicit def hashIterable[T, C[_]](implicit ht: Hash[T], ti: C[T] => Iterable[T]): Hash[C[T]] =
      new Hash[C[T]] {
        override def hash(x: C[T]): Int = {
          val seed = MurmurHash3Compat.seed(x.getClass.hashCode())
          val xs = ti(x)
          val h = xs.foldLeft(seed)((h, p) => MurmurHash3.mix(h, ht.hash(p)))
          MurmurHash3.finalizeHash(h, xs.size)
        }

        override def eqv(x: C[T], y: C[T]): Boolean = {
          val xs = ti(x)
          val ys = ti(y)
          xs.size == ys.size && (xs zip ys).forall((ht.eqv _).tupled)
        }
      }

    implicit def showIterable[T, C[_]](implicit st: Show[T], ti: C[T] => Iterable[T]): Show[C[T]] =
      Show.show(x => ti(x).map(st.show).mkString("[", ",", "]"))

    test[Collections]
    test[MoreCollections]
  }

  {
    // Hash is preferred from Eq
//  implicit val eqJavaEnum: Eq[JavaEnums.Color] = Eq.by(_.name())
//  implicit val eqScalaEnum: Eq[ScalaEnums.Color.Type] = Eq.by(_.toString)
//  implicit val eqAdt: Eq[ADT.Color] = EqDerivation[ADT.Color]

    implicit val hashScalaEnum: Hash[ScalaEnums.Color.Type] = Hash.by(_.toString)
    implicit val hashJavaEnum: Hash[JavaEnums.Color] = Hash.by(_.name())
    implicit val hashAdt: Hash[ADT.Color] = HashDerivation[ADT.Color]

    implicit val showScalaEnum: Show[ScalaEnums.Color.Type] = Show.show(_.toString)
    implicit val showJavaEnum: Show[JavaEnums.Color] = Show.show(_.toString)
    implicit val showAdt: Show[ADT.Color] = ShowDerivation[ADT.Color]

    test[Enums]
  }
}

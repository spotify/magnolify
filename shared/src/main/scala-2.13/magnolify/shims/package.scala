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
package magnolify

import scala.collection.{mutable, Factory}
import scala.util.hashing.MurmurHash3

package object shims {
  trait Monadic[F[_]] extends mercator.Monadic[F] {
    def flatMapS[A, B](from: F[A])(fn: A => F[B]): F[B]
    def mapS[A, B](from: F[A])(fn: A => B): F[B]

    override def flatMap[A, B](from: F[A])(fn: A => F[B]): F[B] = flatMapS(from)(fn)
    override def map[A, B](from: F[A])(fn: A => B): F[B] = mapS(from)(fn)
  }

  trait FactoryCompat[-A, +C] extends Serializable {
    def newBuilder: mutable.Builder[A, C]
    def build(xs: IterableOnce[A]): C = newBuilder.addAll(xs).result()
  }

  object FactoryCompat {
    implicit def fromFactory[A, C](implicit f: Factory[A, C]): FactoryCompat[A, C] =
      new FactoryCompat[A, C] {
        override def newBuilder: mutable.Builder[A, C] = f.newBuilder
      }
  }

  object SerializableCanBuildFroms

  val JavaConverters = scala.jdk.CollectionConverters

  object MurmurHash3Compat {
    def seed(data: Int): Int = MurmurHash3.mix(MurmurHash3.productSeed, data)
  }
}

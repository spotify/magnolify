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

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.language.higherKinds
import scala.reflect.ClassTag

package object shims {
  trait Monadic[F[_]] extends mercator.Monadic[F] {
    def flatMapS[A, B](from: F[A])(fn: A => F[B]): F[B]
    def mapS[A, B](from: F[A])(fn: A => B): F[B]

    override def flatMap[A, B](from: F[A], fn: A => F[B]): F[B] = flatMapS(from)(fn)
    override def map[A, B](from: F[A], fn: A => B): F[B] = mapS(from)(fn)
  }

  trait FactoryCompat[-A, +C] extends Serializable { self =>
    def newBuilder: mutable.Builder[A, C]
    def build(xs: TraversableOnce[A]): C = (newBuilder ++= xs).result()
  }

  object FactoryCompat extends LowPriorityFactoryCompat1 {
    private type FC[A, C] = FactoryCompat[A, C]

    def apply[A, C](f: () => mutable.Builder[A, C]): FC[A, C] =
      new FactoryCompat[A, C] {
        override def newBuilder: mutable.Builder[A, C] = f()
      }

    implicit def arrayFC[A: ClassTag] = FactoryCompat(() => Array.newBuilder[A])
    // Deprecated in 2.13
    // implicit def traversableFC[A] = FactoryCompat(() => Traversable.newBuilder[A])
    // List <: Iterable
    // implicit def iterableFC[A] = FactoryCompat(() => Iterable.newBuilder[A])
    // List <: Seq
    // implicit def seqFC[A] = FactoryCompat(() => Seq.newBuilder[A])
    // Vector <: IndexedSeq
    // implicit def indexedSeqFC[A] = FactoryCompat(() => IndexedSeq.newBuilder[A])
  }

  trait LowPriorityFactoryCompat1 extends LowPriorityFactoryCompat2 {
    implicit def listFC[A] = FactoryCompat(() => List.newBuilder[A])
  }

  trait LowPriorityFactoryCompat2 {
    implicit def vectorFC[A] = FactoryCompat(() => Vector.newBuilder[A])
    // Deprecated in 2.13
    // implicit def streamFC[A] = FactoryCompat(() => Stream.newBuilder[A])
  }

  object SerializableCanBuildFroms {
    private def cbf[A, C](f: () => mutable.Builder[A, C]): CanBuildFrom[C, A, C] =
      new CanBuildFrom[C, A, C] with Serializable {
        override def apply(from: C): mutable.Builder[A, C] = f()
        override def apply(): mutable.Builder[A, C] = f()
      }

    implicit def arrayCBF[A: ClassTag] = cbf(() => Array.newBuilder[A])
    implicit def traversableCBF[A] = cbf(() => Traversable.newBuilder[A])
    implicit def iterableCBF[A] = cbf(() => Iterable.newBuilder[A])
    implicit def seqCBF[A] = cbf(() => Seq.newBuilder[A])
    implicit def indexedSeqCBF[A] = cbf(() => IndexedSeq.newBuilder[A])
    implicit def listCBF[A] = cbf(() => List.newBuilder[A])
    implicit def vectorCBF[A] = cbf(() => Vector.newBuilder[A])
    implicit def streamCBF[A] = cbf(() => Stream.newBuilder[A])
  }
}

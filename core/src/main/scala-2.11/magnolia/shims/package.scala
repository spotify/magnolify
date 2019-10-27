package magnolia

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

  object FactoryCompat {
    private type FC[A, C] = FactoryCompat[A, C]

    def apply[A, C](f: () => mutable.Builder[A, C]): FC[A, C] =
      new FactoryCompat[A, C] {
        override def newBuilder: mutable.Builder[A, C] = f()
      }

    implicit def arrayFC[A: ClassTag] = FactoryCompat(() => Array.newBuilder[A])
    implicit def traversableFC[A] = FactoryCompat(() => Traversable.newBuilder[A])
    implicit def iterableFC[A] = FactoryCompat(() => Iterable.newBuilder[A])
    implicit def seqFC[A] = FactoryCompat(() => Seq.newBuilder[A])
    implicit def indexedSeqFC[A] = FactoryCompat(() => IndexedSeq.newBuilder[A])
    implicit def listFC[A] = FactoryCompat(() => List.newBuilder[A])
    implicit def vectorFC[A] = FactoryCompat(() => Vector.newBuilder[A])
    implicit def streamFC[A] = FactoryCompat(() => Stream.newBuilder[A])
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

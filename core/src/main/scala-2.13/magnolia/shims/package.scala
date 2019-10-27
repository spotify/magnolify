package magnolia

import scala.collection.{Factory, mutable}

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
}

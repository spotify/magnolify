package magnolia

import scala.language.higherKinds

package object shims {
  trait Monadic[F[_]] extends mercator.Monadic[F] {
    def flatMapS[A, B](from: F[A])(fn: A => F[B]): F[B]
    def mapS[A, B](from: F[A])(fn: A => B): F[B]

    override def flatMap[A, B](from: F[A], fn: A => F[B]): F[B] = flatMapS(from)(fn)
    override def map[A, B](from: F[A], fn: A => B): F[B] = mapS(from)(fn)
  }
}

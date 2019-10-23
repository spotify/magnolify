package magnolia.scalacheck.semiauto

import magnolia._
import magnolia.shims.Monadic
import org.scalacheck.{Arbitrary, Gen}

import scala.language.experimental.macros

object ArbitraryDerivation {
  type Typeclass[T] = Arbitrary[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Arbitrary {
    Gen.lzy(caseClass.constructMonadic(_.typeclass.arbitrary)(monadicGen))
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = Arbitrary {
    Gen.oneOf(sealedTrait.subtypes.map(_.typeclass.arbitrary)).flatMap(identity)
  }

  private val monadicGen: Monadic[Gen] = new Monadic[Gen] {
    override def point[A](value: A): Gen[A] = Gen.const(value)
    override def flatMapS[A, B](from: Gen[A])(fn: A => Gen[B]): Gen[B] = from.flatMap(fn)
    override def mapS[A, B](from: Gen[A])(fn: A => B): Gen[B] = from.map(fn)
  }

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]
}

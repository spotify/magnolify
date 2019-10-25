package magnolia.scalacheck.semiauto

import magnolia._
import magnolia.shims.Monadic
import org.scalacheck.{Arbitrary, Gen}

import scala.language.experimental.macros

object ArbitraryDerivation {
  type Typeclass[T] = Arbitrary[T]

  def combine[T: Fallback](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Arbitrary {
    Gen.lzy(Gen.sized { size =>
      if (size >= 0) {
        Gen.resize(size - 1,
          caseClass.constructMonadic(_.typeclass.arbitrary)(monadicGen))
      } else {
        implicitly[Fallback[T]].get
      }
    })
  }

  def dispatch[T: Fallback](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = Arbitrary {
    Gen.sized { size =>
      if (size > 0) {
        Gen.resize(size - 1,
          Gen.oneOf(sealedTrait.subtypes.map(_.typeclass.arbitrary)).flatMap(identity))
      } else {
        implicitly[Fallback[T]].get
      }
    }
  }

  private val monadicGen: Monadic[Gen] = new Monadic[Gen] {
    override def point[A](value: A): Gen[A] = Gen.const(value)
    override def flatMapS[A, B](from: Gen[A])(fn: A => Gen[B]): Gen[B] = from.flatMap(fn)
    override def mapS[A, B](from: Gen[A])(fn: A => B): Gen[B] = from.map(fn)
  }

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]

  sealed trait Fallback[+T] extends Serializable {
    def get: Gen[T]
  }

  object Fallback {
    def apply[T](g: Gen[T]): Fallback[T] = new Fallback[T] {
      override def get: Gen[T] = g
    }

    def apply[T](v: T): Fallback[T] = Fallback[T](Gen.const(v))
    def apply[T](implicit arb: Arbitrary[T]): Fallback[T] = Fallback[T](arb.arbitrary)

    implicit def defaultFallback[T]: Fallback[T] = Fallback[T](Gen.fail)
  }
}

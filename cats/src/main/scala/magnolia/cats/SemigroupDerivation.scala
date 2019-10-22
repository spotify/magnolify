package magnolia.cats

import cats.Semigroup
import magnolia._

import scala.language.experimental.macros

object SemigroupDerivation {
  type Typeclass[T] = Semigroup[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Semigroup.instance { (x, y) =>
    caseClass.construct { p =>
      p.typeclass.combine(p.dereference(x), p.dereference(y))
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    Semigroup.instance { (x, y) =>
      val sub = sealedTrait.subtypes.find(_.cast.isDefinedAt(x)).get
      if (sub.cast.isDefinedAt(y)) {
        sub.typeclass.combine(sub.cast(x), sub.cast(y))
      } else {
        throw new IllegalArgumentException(s"Incompatible subtypes $x and $y")
      }
    }

  implicit def gen[T]: Semigroup[T] = macro Magnolia.gen[T]
}

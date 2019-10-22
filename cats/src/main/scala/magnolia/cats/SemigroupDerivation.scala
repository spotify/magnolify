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

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}

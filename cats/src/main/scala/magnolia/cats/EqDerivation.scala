package magnolia.cats

import cats.Eq
import magnolia._

import scala.language.experimental.macros

object EqDerivation {
  type Typeclass[T] = Eq[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Eq.instance[T] { (x, y) =>
    caseClass.parameters.forall { p =>
      p.typeclass.eqv(p.dereference(x), p.dereference(y))
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Eq[T] = Eq.instance[T] { (x, y) =>
    sealedTrait.dispatch(x) { sub =>
      sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.cast(x), sub.cast(y))
    }
  }

  implicit def gen[T]: Eq[T] = macro Magnolia.gen[T]
}

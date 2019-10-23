package magnolia.cats.semiauto

import cats.Eq
import magnolia._

import scala.language.experimental.macros

object EqDerivation {
  type Typeclass[T] = Eq[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Eq.instance { (x, y) =>
    caseClass.parameters.forall { p =>
      p.typeclass.eqv(p.dereference(x), p.dereference(y))
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = Eq.instance { (x, y) =>
    sealedTrait.dispatch(x) { sub =>
      sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.cast(x), sub.cast(y))
    }
  }

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]
}

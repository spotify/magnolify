package magnolia.scalacheck

import magnolia._
import org.scalacheck.Cogen

import scala.language.experimental.macros

object CogenDerivation {
  type Typeclass[T] = Cogen[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Cogen { (seed, t) =>
    caseClass.parameters.foldLeft(seed) { (s, p) =>
      p.typeclass.perturb(s, p.dereference(t))
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = Cogen { (seed, t: T) =>
    sealedTrait.dispatch(t) { sub =>
      // inject index to separate case objects instances
      val s = Cogen.cogenInt.perturb(seed, sub.index)
      sub.typeclass.perturb(s, sub.cast(t))
    }
  }

  implicit def gen[T]: Cogen[T] = macro Magnolia.gen[T]
}

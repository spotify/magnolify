package magnolia.scalacheck

import magnolia._
import org.scalacheck._

import scala.language.experimental.macros

object CogenDerivation {
  type Typeclass[T] = Cogen[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Cogen { (seed, t) =>
    caseClass.parameters.foldLeft(seed) { (s, p) =>
      p.typeclass.perturb(s, p.dereference(t))
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = Cogen { (seed, t: T) =>
    val p = sealedTrait.subtypes.find(_.cast.isDefinedAt(t)).get
    // inject index to separate case objects instances
    val s = Cogen.cogenInt.perturb(seed, p.index)
    p.typeclass.perturb(s, p.cast(t))
  }

  implicit def gen[T]: Cogen[T] = macro Magnolia.gen[T]
}

package magnolify.cats.semiauto

import cats.Hash
import magnolia._

import scala.language.experimental.macros
import scala.util.hashing.MurmurHash3

object HashDerivation {
  type Typeclass[T] = Hash[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Hash[T] {

    override def hash(x: T): Int = {
      val h = caseClass.parameters.foldLeft(MurmurHash3.productSeed) { (h, p) =>
        MurmurHash3.mix(h, p.typeclass.hash(p.dereference(x)))
        //      h ^ p.typeclass.hash(p.dereference(x))
      }
      MurmurHash3.finalizeHash(h, caseClass.parameters.size)
    }

    override def eqv(x: T, y: T): Boolean = caseClass.parameters.forall { p =>
      p.typeclass.eqv(p.dereference(x), p.dereference(y))
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = new Hash[T] {
    override def hash(x: T): Int = sealedTrait.dispatch(x) { sub =>
      sub.index ^ sub.typeclass.hash(sub.cast(x))
    }

    override def eqv(x: T, y: T): Boolean = sealedTrait.dispatch(x) { sub =>
      sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.cast(x), sub.cast(y))
    }
  }

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]
}

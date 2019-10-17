package magnolia.cats

import cats.Semigroup
import magnolia._

import scala.language.experimental.macros

object SemigroupDerivation {
  type Typeclass[T] = Semigroup[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Semigroup[T] {
    override def combine(x: T, y: T): T =
      caseClass.construct { p =>
        p.typeclass.combine(p.dereference(x), p.dereference(y))
      }

    //override def combineAllOption(as: IterableOnce[T]): Option[T] = ???
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = new Semigroup[T] {
    override def combine(x: T, y: T): T = ???
    //override def combineAllOption(as: IterableOnce[T]): Option[T] = ???
  }

  implicit def gen[T]: Semigroup[T] = macro Magnolia.gen[T]
}

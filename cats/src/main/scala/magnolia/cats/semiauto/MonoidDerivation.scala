package magnolia.cats.semiauto

import cats.Monoid
import magnolia._

import scala.language.experimental.macros

object MonoidDerivation {
  type Typeclass[T] = Monoid[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = {
    val empty = caseClass.construct(_.typeclass.empty)
    val combine = (x: T, y: T) => caseClass.construct { p =>
      p.typeclass.combine(p.dereference(x), p.dereference(y))
    }
    Monoid.instance(empty, combine)
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def apply[T]: Typeclass[T] = macro Magnolia.gen[T]
}

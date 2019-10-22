package magnolia.cats

import cats.Group
import magnolia._

import scala.language.experimental.macros

object GroupDerivation {
  type Typeclass[T] = Group[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = new Group[T] {
    override def inverse(a: T): T = caseClass.construct { p =>
      p.typeclass.inverse(p.dereference(a))
    }

    override def empty: T = caseClass.construct(_.typeclass.empty)

    override def combine(x: T, y: T): T = caseClass.construct { p =>
      p.typeclass.combine(p.dereference(x), p.dereference(y))
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Group[T] = macro Magnolia.gen[T]
}

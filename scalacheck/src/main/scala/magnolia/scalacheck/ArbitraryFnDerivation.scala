package magnolia.scalacheck

import magnolia._
import org.scalacheck._

import scala.language.experimental.macros

object ArbitraryFnDerivation {
  type Typeclass[T] = Arbitrary[T => T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = Arbitrary {
    type P = Param[Typeclass, T]

    def fn(param: P)(f: param.PType => param.PType): T => Any =
      (t: T) => f(param.dereference(t))

    def traverse(xs: List[P]): Gen[List[T => Any]] = xs match {
      case Nil => Gen.const(Nil)
      case head :: tail => head.typeclass.arbitrary.flatMap { f =>
        traverse(tail).map(fn(head)(f) :: _)
      }
    }

    val fieldF = traverse(caseClass.parameters.toList)
    fieldF.map { fs =>
      (t: T) => caseClass.rawConstruct(fs.map(_(t)))
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = Arbitrary {
    type P = Subtype[Typeclass, T]

    def fn(param: P)(f: param.SType => param.SType): PartialFunction[T, T] =
      new PartialFunction[T, T] {
        override def isDefinedAt(x: T): Boolean = param.cast.isDefinedAt(x)
        override def apply(x: T): T = f(param.cast(x))
      }

    def traverse(xs: List[P]): Gen[List[PartialFunction[T, T]]] = xs match {
      case Nil => Gen.const(Nil)
      case head :: tail => head.typeclass.arbitrary.flatMap { f =>
        traverse(tail).map(fn(head)(f) :: _)
      }
    }

    val subtypeF = traverse(sealedTrait.subtypes.toList)
    subtypeF.map { fs =>
      (t: T) => fs.find(_.isDefinedAt(t)).get.apply(t)
    }
  }

  implicit def gen[T]: Arbitrary[T => T] = macro Magnolia.gen[T]
}

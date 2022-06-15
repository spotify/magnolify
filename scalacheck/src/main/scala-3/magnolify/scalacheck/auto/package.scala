package magnolify.scalacheck

import magnolify.scalacheck.semiauto.{ArbitraryDerivation, CogenDerivation}
import org.scalacheck.{Arbitrary, Cogen}

import scala.deriving.Mirror

package object auto extends AutoDerivation

trait AutoDerivation:
  inline given autoDerivedArbitrary[T](using Mirror.Of[T]): Arbitrary[T] = ArbitraryDerivation[T]
  inline given autoDerivedCogen[T](using Mirror.Of[T]): Cogen[T] = CogenDerivation[T]

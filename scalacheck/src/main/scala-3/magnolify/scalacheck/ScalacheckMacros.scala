package magnolify.scalacheck

import org.scalacheck.{Arbitrary, Cogen}

import scala.deriving.Mirror
trait AutoDerivations:

  inline given genArbitrary[T](using Mirror.Of[T]): Arbitrary[T] = ArbitraryDerivation.derivedMirror[T]
  inline given genCogen[T](using Mirror.Of[T]): Cogen[T] = CogenDerivation.derivedMirror[T]

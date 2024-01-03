package magnolify.scalacheck

import org.scalacheck.{Arbitrary, Cogen}

import scala.deriving.Mirror

trait AutoDerivations:
  inline implicit def genArbitrary[T](using Mirror.Of[T]): Arbitrary[T] =
    ArbitraryDerivation.derivedMirror[T]
  inline implicit def genCogen[T](using Mirror.Of[T]): Cogen[T] =
    CogenDerivation.derivedMirror[T]

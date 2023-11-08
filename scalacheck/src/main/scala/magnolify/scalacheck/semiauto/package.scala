package magnolify.scalacheck

import org.scalacheck.{Arbitrary, Cogen}

package object semiauto {

  @deprecated("Use Arbitrary.gen[T] instead", "0.7.0")
  val ArbitraryDerivation = magnolify.scalacheck.ArbitraryDerivation
  @deprecated("Use Gogen.gen[T] instead", "0.7.0")
  val CogenDerivation = magnolify.scalacheck.CogenDerivation

  implicit def genArbitrary(a: Arbitrary.type): magnolify.scalacheck.ArbitraryDerivation.type =
    magnolify.scalacheck.ArbitraryDerivation
  implicit def genCogen(c: Cogen.type): magnolify.scalacheck.CogenDerivation.type =
    magnolify.scalacheck.CogenDerivation
}

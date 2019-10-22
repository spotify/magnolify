package magnolia.scalacheck.test

import magnolia.test.Simple._
import org.scalacheck._

object ScopeTest {
  object Auto {
    import magnolia.scalacheck._
    implicitly[Arbitrary[Numbers]]
    implicitly[Cogen[Numbers]]
    implicitly[Arbitrary[Numbers => Numbers]]
  }

  object Semi {
    import magnolia.scalacheck.ArbitraryDerivation
    import magnolia.scalacheck.CogenDerivation
    implicit val arb: Arbitrary[Numbers] = ArbitraryDerivation.gen[Numbers]
    implicit val cogen: Cogen[Numbers] = CogenDerivation.gen[Numbers]
    // T => T is not a case class, so ArbitraryDerivation.gen won't work
    implicitly[Arbitrary[Numbers => Numbers]]
  }
}

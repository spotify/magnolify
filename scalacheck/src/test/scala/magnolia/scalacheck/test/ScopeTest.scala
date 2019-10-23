package magnolia.scalacheck.test

import magnolia.test.Simple._
import org.scalacheck._

object ScopeTest {
  object Auto {
    import magnolia.scalacheck.auto._
    implicitly[Arbitrary[Numbers]]
    implicitly[Cogen[Numbers]]
    implicitly[Arbitrary[Numbers => Numbers]]
  }

  object Semi {
    import magnolia.scalacheck.semiauto._
    implicit val arb: Arbitrary[Numbers] = ArbitraryDerivation[Numbers]
    implicit val cogen: Cogen[Numbers] = CogenDerivation[Numbers]
    // T => T is not a case class, so ArbitraryDerivation.apply won't work
    implicitly[Arbitrary[Numbers => Numbers]]
  }
}

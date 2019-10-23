package magnolia.cats.test

import cats._
import cats.instances.all._
import magnolia.test.Simple._

object ScopeTest {
  object Auto {
    import magnolia.cats.auto._
    implicitly[Eq[Numbers]]
    implicitly[Semigroup[Numbers]]
    implicitly[Monoid[Numbers]]
    implicitly[Group[Numbers]]
  }

  object Semi {
    import magnolia.cats.semiauto._
    EqDerivation[Numbers]
    SemigroupDerivation[Numbers]
    MonoidDerivation[Numbers]
    GroupDerivation[Numbers]
  }
}

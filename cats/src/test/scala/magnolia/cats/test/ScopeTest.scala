package magnolia.cats.test

import cats._
import cats.instances.all._
import magnolia.test.Simple._

object ScopeTest {
  object Auto {
    import magnolia.cats._
    implicitly[Eq[Numbers]]
    implicitly[Semigroup[Numbers]]
  }

  object Semi {
    import magnolia.cats.EqDerivation
    import magnolia.cats.SemigroupDerivation
    EqDerivation.gen[Numbers]
    SemigroupDerivation.gen[Numbers]
  }
}

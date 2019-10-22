package magnolia.cats.test

import cats._
import cats.instances.all._
import magnolia.test.Simple._

object ScopeTest {
  object Auto {
    import magnolia.cats._
    implicitly[Eq[Numbers]]
    implicitly[Semigroup[Numbers]]
    implicitly[Monoid[Numbers]]
//    implicitly[Group[Numbers]]
  }

  object Semi {
    import magnolia.cats.EqDerivation
    import magnolia.cats.SemigroupDerivation
    import magnolia.cats.MonoidDerivation
    import magnolia.cats.GroupDerivation
    EqDerivation.gen[Numbers]
    SemigroupDerivation.gen[Numbers]
    MonoidDerivation.gen[Numbers]
    GroupDerivation.gen[Numbers]
  }
}

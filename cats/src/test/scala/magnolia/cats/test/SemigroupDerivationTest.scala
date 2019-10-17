package magnolia.cats.test

import cats._
import cats.instances.all._
import cats.kernel.laws.discipline._
import magnolia.cats._
import magnolia.scalacheck._
import magnolia.test.Simple._
import org.scalacheck._

object SemigroupDerivationTest extends Properties("SemigroupDerivation") {
  include(SemigroupTests[Integers].semigroup.all)
}

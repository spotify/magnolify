package magnolia.scalacheck.test

import magnolia.scalacheck.auto._
import magnolia.test.ADT._
import magnolia.test.Simple._
import magnolia.test._
import org.scalacheck._
import org.scalacheck.rng.Seed

import scala.reflect._

object CogenDerivationSpec extends MagnoliaSpec("CogenDerivation") {
  private def test[T: Arbitrary : ClassTag : Cogen]: Unit = include(props[T])
  private def test[T: Arbitrary : ClassTag : Cogen](seed: Long): Unit =
    includeWithSeed(props[T], seed)

  private def props[T: ClassTag](implicit arb: Arbitrary[T], cogen: Cogen[T]): Properties = {
    ensureSerializable(cogen)
    new Properties(className[T]) {
      implicit val arbList = Arbitrary(Gen.listOfN(10, arb.arbitrary))
      property("uniqueness") = Prop.forAll { (seed: Seed, xs: List[T]) =>
        xs.map(cogen.perturb(seed, _)).toSet.size == xs.toSet.size
      }
      property("consistency") = Prop.forAll { (seed: Seed, x: T) =>
        cogen.perturb(seed, x) == cogen.perturb(seed, x)
      }
    }
  }

  test[Numbers]
  test[Required]
  // FIXME: not enough unique results due to None/Nil
  test[Nullable](0)
  test[Repeated](0)
  test[Nested]

  import Custom._
  test[Custom]

  test[Node](0)
  test[GNode[Int]](0)
  test[Shape]
  test[Color]
}

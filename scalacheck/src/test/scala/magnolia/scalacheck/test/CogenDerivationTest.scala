package magnolia.scalacheck.test

import magnolia.test.Simple._
import magnolia.test.ADT._
import magnolia.scalacheck._
import org.scalacheck._
import org.scalacheck.rng.Seed
import org.scalatest._

class CogenDerivationTest extends FlatSpec with Matchers {
  private def test[T: Arbitrary](implicit cogen: Cogen[T]): Unit = {
    val gen = implicitly[Arbitrary[T]].arbitrary

    val xs = Gen.listOfN(100, gen).sample.get
    xs.map(cogen.perturb(Seed.random(), _)).toSet.size == xs.toSet.size
  }

  ////////////////////////////////////////
  // Simple types
  ////////////////////////////////////////

  "CogenDerivation" should "work with Numbers" in {
    test[Numbers]
  }

  it should "work with Required" in {
    test[Required]
  }

  it should "work with Nullable" in {
    test[Nullable]
  }

  it should "work with Repeated" in {
    test[Repeated]
  }

  it should "work with Nested" in {
    test[Nested]
  }

  it should "work with Custom" in {
    import Custom._
    test[Custom]
  }

  ////////////////////////////////////////
  // ADTs
  ////////////////////////////////////////

  it should "work with Node" in {
    test[Node]
  }

  it should "work with GNode" in {
    test[GNode[Int]]
  }

  it should "work with Shape" in {
    test[Shape]
  }

  it should "work with Color" in {
    test[Color]
  }
}

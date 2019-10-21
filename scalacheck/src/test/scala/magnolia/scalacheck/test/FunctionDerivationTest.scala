package magnolia.scalacheck.test

import magnolia.test.Simple._
import magnolia.test.ADT._
import magnolia.scalacheck._
import org.scalacheck._
import org.scalatest._

class FunctionDerivationTest extends FlatSpec with Matchers {
  private def test[A: Arbitrary, B](implicit actual: Arbitrary[A => B]): Unit = {
    val aGen = implicitly[Arbitrary[A]].arbitrary
    val fGen = actual.arbitrary

    val x = aGen.sample.get
    val xs = Gen.listOfN(100, aGen).sample.get
    val f = fGen.sample.get
    val fs = Gen.listOfN(100, fGen).sample.get

    // same input => same output
    (1 to 100).map(_ => f(x)).toSet.size shouldBe 1

    // different inputs, same function
    xs.map(f).toSet.size should be > 1

    // same input, different functions
    fs.map(_(x)).toSet.size should be > 1
  }

  "Function derivation" should "work with same types" in {
    test[Numbers, Numbers]
    test[Shape, Shape]
    test[Color, Color]
  }

  it should "work with mixed types" in {
    test[Numbers, Shape]
    test[Shape, Numbers]
    test[Numbers, Color]
    test[Color, Numbers]
  }
}

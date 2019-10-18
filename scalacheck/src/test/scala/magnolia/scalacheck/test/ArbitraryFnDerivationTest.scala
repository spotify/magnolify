package magnolia.scalacheck.test

import com.google.protobuf.ByteString
import magnolia.test.Simple._
import magnolia.test.ADT._
import magnolia.scalacheck._
import org.joda.time.Instant
import org.scalacheck._
import org.scalatest._

class ArbitraryFnDerivationTest extends FlatSpec with Matchers {
  private def test[T: Arbitrary](implicit actual: Arbitrary[T => T]): Unit = {
    val tGen = implicitly[Arbitrary[T]].arbitrary
    val fGen = actual.arbitrary

    val x = tGen.sample.get
    val xs = Gen.listOfN(100, tGen).sample.get
    val f = fGen.sample.get
    val fs = Gen.listOfN(100, fGen).sample.get

    // same input => same output
    (1 to 100).map(_ => f(x)).toSet.size shouldBe 1

    // different inputs, same function
    xs.map(f).toSet.size should be > 1

    // same input, different functions
    fs.map(_(x)).toSet.size should be > 1
  }

  ////////////////////////////////////////
  // Simple types
  ////////////////////////////////////////

  "ArbitraryFnDerivation" should "work with Numbers" in {
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

  it should "work with custom implicits" in {
    implicit val arbIntFn: Arbitrary[Int => Int] = Arbitrary {
      Arbitrary.arbInt.arbitrary.map(i => (x: Int) => x + i)
    }
    test[Required]
    val f = arbIntFn.arbitrary.sample.get
    (1 to 100).map(x => f(x) - x).toSet.size shouldBe 1
  }

  it should "work with Custom" in {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val arbInstant: Arbitrary[Instant] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(new Instant(_)))
    implicit val coByteString: Cogen[ByteString] = Cogen(_.hashCode())
    implicit val coInstant: Cogen[Instant] = Cogen(_.getMillis)
    test[Custom]
  }
  ////////////////////////////////////////
  // ADTs
  ////////////////////////////////////////

  // FIXME: StackOverflowError for recursive ADTs, T => T should cover all sub-types

  ignore should "work with Node" in {
    test[Node]
  }

  ignore should "work with GNode" in {
    test[GNode[Int]]
  }

  ignore should "work with Shape" in {
    test[Shape]
  }

  ignore should "work with Color" in {
    test[Color]
  }
}

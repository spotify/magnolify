package magnolia.scalacheck.test

import com.google.protobuf.ByteString
import magnolia.test.Simple._
import magnolia.test.ADT._
import magnolia.scalacheck._
import org.joda.time.Duration
import org.scalacheck._
import org.scalatest._

class ArbitraryDerivationTest extends FlatSpec with Matchers {
  private val parameters = Gen.Parameters.default
  private val seed = rng.Seed.random()

  private def test[T](expected: Gen[T])(implicit actual: Arbitrary[T]): Unit = {
    (stream(actual.arbitrary) zip stream(expected)).take(100).foreach { case (x, y) =>
      x shouldEqual y
    }
  }

  private def stream[T](g: Gen[T]): Stream[T] = {
    def mkStream(s: rng.Seed): Stream[T] = {
      val r = g.doPureApply(parameters, s, 0)
      r.retrieve.get #:: mkStream(r.seed)
    }
    mkStream(seed)
  }

  ////////////////////////////////////////
  // Simple types
  ////////////////////////////////////////

  "ArbitraryDerivation" should "work with Numbers" in {
    test(for {
      i <- Arbitrary.arbInt.arbitrary
      l <- Arbitrary.arbLong.arbitrary
      f <- Arbitrary.arbFloat.arbitrary
      d <- Arbitrary.arbDouble.arbitrary
      bi <- Arbitrary.arbBigInt.arbitrary
      bd <- Arbitrary.arbBigDecimal.arbitrary
    } yield Numbers(i, l, f, d, bi, bd))
  }

  it should "work with Required" in {
    test(for {
      b <- Arbitrary.arbBool.arbitrary
      i <- Arbitrary.arbInt.arbitrary
      s <- Arbitrary.arbString.arbitrary
    } yield Required(b, i, s))
  }

  it should "work with Nullable" in {
    test(for {
      b <- Gen.option(Arbitrary.arbBool.arbitrary)
      i <- Gen.option(Arbitrary.arbInt.arbitrary)
      s <- Gen.option(Arbitrary.arbString.arbitrary)
    } yield Nullable(b, i, s))
  }

  it should "work with Repeated" in {
    test(for {
      b <- Gen.listOf(Arbitrary.arbBool.arbitrary)
      i <- Gen.listOf(Arbitrary.arbInt.arbitrary)
      s <- Gen.listOf(Arbitrary.arbString.arbitrary)
    } yield Repeated(b, i, s))
  }

  it should "work with Nested" in {
    test(for {
      b <- Arbitrary.arbBool.arbitrary
      i <- Arbitrary.arbInt.arbitrary
      s <- Arbitrary.arbString.arbitrary
      nb <- Arbitrary.arbBool.arbitrary
      ni <- Arbitrary.arbInt.arbitrary
      ns <- Arbitrary.arbString.arbitrary
    } yield Nested(b, i, s, Required(nb, ni, ns)))
  }

  it should "work with custom implicits" in {
    implicit val arbInt: Arbitrary[Int] = Arbitrary(Gen.chooseNum(0, 100))
    implicit val arbStr: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)
    test(for {
      b <- Arbitrary.arbBool.arbitrary
      i <- Gen.chooseNum(0, 100)
      s <- Gen.alphaNumStr
    } yield Required(b, i, s))
  }

  it should "work with Custom" in {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val arbInstant: Arbitrary[Duration] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Duration.millis(_)))
    test(for {
      b <- Gen.alphaNumStr
      i <- Gen.chooseNum(0, Int.MaxValue)
    } yield Custom(ByteString.copyFromUtf8(b), Duration.millis(i)))
  }

  ////////////////////////////////////////
  // ADTs
  ////////////////////////////////////////

  it should "work with Node" in {
    test(Node.gen)
  }

  it should "work with GNode" in {
    test(GNode.gen[Int])
  }

  it should "work with Shape" in {
    val genSpace = Gen.const(Space)
    val genPoint = for {
      x <- Arbitrary.arbInt.arbitrary
      y <- Arbitrary.arbInt.arbitrary
    } yield Point(x, y)
    val genCircle = for {
      r <- Arbitrary.arbInt.arbitrary
    } yield Circle(r)
    val genShape = Gen.oneOf[Shape](genCircle, genPoint, genSpace)
    test(genShape)
  }

  it should "work with Color" in {
    test(Gen.oneOf[Color](Blue, Green, Red))
  }
}

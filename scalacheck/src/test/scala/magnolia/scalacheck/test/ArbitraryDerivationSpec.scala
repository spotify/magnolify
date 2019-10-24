package magnolia.scalacheck.test

import java.net.URI
import java.time.Duration

import magnolia.scalacheck.auto._
import magnolia.test.ADT._
import magnolia.test.Simple._
import magnolia.test._
import org.scalacheck._
import org.scalacheck.rng.Seed
import org.scalacheck.util.SerializableCanBuildFroms._

import scala.reflect._

object ArbitraryDerivationSpec extends MagnoliaSpec("ArbitraryDerivation") {
  private val parameters = Gen.Parameters.default

  private def test[T: Arbitrary : ClassTag](expected: Gen[T]): Unit =
    include(props[T](expected))
  private def test[T: Arbitrary : ClassTag](prefix: String)(expected: Gen[T]): Unit =
    include(props[T](expected), prefix)
  private def test[T: Arbitrary : ClassTag](seed: Long)(expected: Gen[T]): Unit =
    includeWithSeed(props[T](expected), 0)

  private def props[T: ClassTag](expected: Gen[T])(implicit arb: Arbitrary[T]): Properties = {
    ensureSerializable(arb)
    val actual = arb.arbitrary
    new Properties(className[T]) {
      property("eqv") = Prop.forAll { seed: Seed =>
        actual(parameters, seed) == expected(parameters, seed)
      }
    }
  }

  test(for {
    i <- Arbitrary.arbInt.arbitrary
    l <- Arbitrary.arbLong.arbitrary
    f <- Arbitrary.arbFloat.arbitrary
    d <- Arbitrary.arbDouble.arbitrary
    bi <- Arbitrary.arbBigInt.arbitrary
    bd <- Arbitrary.arbBigDecimal.arbitrary
  } yield Numbers(i, l, f, d, bi, bd))

  test(for {
    b <- Arbitrary.arbBool.arbitrary
    i <- Arbitrary.arbInt.arbitrary
    s <- Arbitrary.arbString.arbitrary
  } yield Required(b, i, s))

  test(for {
    b <- Gen.option(Arbitrary.arbBool.arbitrary)
    i <- Gen.option(Arbitrary.arbInt.arbitrary)
    s <- Gen.option(Arbitrary.arbString.arbitrary)
  } yield Nullable(b, i, s))

  test(for {
    b <- Gen.listOf(Arbitrary.arbBool.arbitrary)
    i <- Gen.listOf(Arbitrary.arbInt.arbitrary)
    s <- Gen.listOf(Arbitrary.arbString.arbitrary)
  } yield Repeated(b, i, s))

  test(for {
    b <- Arbitrary.arbBool.arbitrary
    i <- Arbitrary.arbInt.arbitrary
    s <- Arbitrary.arbString.arbitrary
    nb <- Arbitrary.arbBool.arbitrary
    ni <- Arbitrary.arbInt.arbitrary
    ns <- Arbitrary.arbString.arbitrary
  } yield Nested(b, i, s, Required(nb, ni, ns)))

  {
    implicit val arbInt: Arbitrary[Int] = Arbitrary(Gen.chooseNum(0, 100))
    implicit val arbStr: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)
    test("implicits.")(for {
      b <- Arbitrary.arbBool.arbitrary
      i <- Gen.chooseNum(0, 100)
      s <- Gen.alphaNumStr
    } yield Required(b, i, s))
  }

  {
    val actual = implicitly[Arbitrary[Collections]].arbitrary
    val expected = for {
      a <- Gen.containerOf[Array, Int](Arbitrary.arbInt.arbitrary)
      l <- Gen.listOf(Arbitrary.arbInt.arbitrary)
      v <- Gen.containerOf[Vector, Int](Arbitrary.arbInt.arbitrary)
    } yield Collections(a, l, v)
    property(className[Collections]) = Prop.forAll { seed: Seed =>
      val x = actual(parameters, seed).get
      val y = expected(parameters, seed).get
      Prop.all(
        x.a.toList == y.a.toList,
        x.l == y.l,
        x.v == y.v)
    }
  }

  {
    implicit val arbUri: Arbitrary[URI] =
      Arbitrary(Gen.alphaNumStr.map(URI.create))
    implicit val arbDuration: Arbitrary[Duration] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Duration.ofMillis(_)))
    test(for {
      u <- Gen.alphaNumStr
      d <- Gen.chooseNum(0, Int.MaxValue)
    } yield Custom(URI.create(u), Duration.ofMillis(d)))
  }

  test(0)(Node.gen)
  test(0)(GNode.gen[Int])

  {
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

  test(Gen.oneOf[Color](Blue, Green, Red))
}

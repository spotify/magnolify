package magnolia.scalacheck.test

import java.net.URI
import java.time.Duration

import magnolia.scalacheck.auto._
import magnolia.shims.SerializableCanBuildFroms._
import magnolia.test.ADT._
import magnolia.test.Simple._
import magnolia.test._
import org.scalacheck._
import org.scalacheck.rng.Seed

import scala.reflect._

object ArbitraryDerivationSpec extends MagnoliaSpec("ArbitraryDerivation") {
  private def test[T: ClassTag](implicit arb: Arbitrary[T]): Unit = test[T, T](identity)

  private def test[T: ClassTag, U](f: T => U)(implicit arb: Arbitrary[T]): Unit = {
    ensureSerializable(arb)
    val name = className[T]
    val g = arb.arbitrary
    val prms = Gen.Parameters.default
    property(s"$name.uniqueness") = Prop.forAll(Gen.listOfN(10, g)) { xs =>
      xs.size > 1
    }
    property(s"$name.consistency") = Prop.forAll { seed: Seed =>
      f(g(prms, seed).get) == f(g(prms, seed).get)
    }
  }

  test[Numbers]
  test[Required]
  test[Nullable]

  {
    // FIXME: uniqueness workaround for Nils
    implicit def arbList[T](implicit arb: Arbitrary[T]): Arbitrary[List[T]] =
      Arbitrary(Gen.nonEmptyListOf(arb.arbitrary))
    test[Repeated]
    test((c: Collections) => (c.a.toList, c.l, c.v))
  }

  test[Nested]

  {
    implicit val arbInt: Arbitrary[Int] = Arbitrary(Gen.chooseNum(0, 100))
    implicit val arbLong: Arbitrary[Long] = Arbitrary(Gen.chooseNum(100, 10000))
    property("implicits") = Prop.forAll { x: Integers =>
      x.i >= 0 && x.i <= 100 && x.l >= 100 && x.l <= 10000
    }
  }

  {
    implicit val arbUri: Arbitrary[URI] =
      Arbitrary(Gen.alphaNumStr.map(URI.create))
    implicit val arbDuration: Arbitrary[Duration] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Duration.ofMillis(_)))
    test[Custom]
  }

  {
    import magnolia.scalacheck.semiauto.ArbitraryDerivation.Fallback
    implicit val f: Fallback[Node] = Fallback[Leaf]
    test[Node]
  }

  {
    import magnolia.scalacheck.semiauto.ArbitraryDerivation.Fallback
    implicit val f: Fallback[GNode[Int]] = Fallback[GLeaf[Int]]
    test[GNode[Int]]
  }

  test[Shape]
  test[Color]
}

package magnolia.scalacheck.test

import magnolia.scalacheck.auto._
import magnolia.test.ADT._
import magnolia.test.Simple._
import magnolia.test._
import org.scalacheck._
import org.scalacheck.rng.Seed

import scala.reflect._

object CogenDerivationSpec extends MagnoliaSpec("CogenDerivation") {
  private def test[T: ClassTag](implicit arb: Arbitrary[T], co: Cogen[T]): Unit =
    test[T, T](identity)

  private def test[T: ClassTag, U](f: T => U)
                                  (implicit arb: Arbitrary[T], co: Cogen[T]): Unit = {
    ensureSerializable(co)
    val name = className[T]
    implicit val arbList: Arbitrary[List[T]] = Arbitrary(Gen.listOfN(10, arb.arbitrary))
    property(s"$name.uniqueness") = Prop.forAll { (seed: Seed, xs: List[T]) =>
      xs.map(co.perturb(seed, _)).toSet.size == xs.map(f).toSet.size
    }
    property(s"$name.consistency") = Prop.forAll { (seed: Seed, x: T) =>
      co.perturb(seed, x) == co.perturb(seed, x)
    }
  }

  test[Numbers]
  test[Required]

  {
    // FIXME: uniqueness workaround for Nones
    implicit def arbOption[T](implicit arb: Arbitrary[T]): Arbitrary[Option[T]] =
      Arbitrary(Gen.frequency(1 -> Gen.const(None), 99 -> Gen.some(arb.arbitrary)))
    test[Nullable]
  }

  {
    // FIXME: uniqueness workaround for Nils
    implicit def arbList[T](implicit arb: Arbitrary[T]): Arbitrary[List[T]] =
      Arbitrary(Gen.nonEmptyListOf(arb.arbitrary))
    test[Repeated]
    test((c: Collections) => (c.a.toList, c.l, c.v))
  }

  test[Nested]

  import Custom._
  test[Custom]

  test[Node]
  test[GNode[Int]]
  test[Shape]
  test[Color]
}

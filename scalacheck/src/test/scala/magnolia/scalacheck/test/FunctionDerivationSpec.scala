package magnolia.scalacheck.test

import magnolia.test.Simple._
import magnolia.test.ADT._
import magnolia.scalacheck.auto._
import magnolia.test._
import org.scalacheck._

import scala.reflect._

object FunctionDerivationSpec extends MagnoliaSpec("FunctionDerivation") {
  private def test[A: Arbitrary : ClassTag, B: ClassTag](implicit arb: Arbitrary[A => B]): Unit = {
    ensureSerializable(arb)
    val name = s"${className[A]}.${className[B]}"
    property(s"$name.consistency") = Prop.forAll { (f: A => B, a: A) =>
      f(a) == f(a)
    }
    implicit def arbList[T](implicit arb: Arbitrary[T]): Arbitrary[List[T]] =
      Arbitrary(Gen.listOfN(100, arb.arbitrary))
    property(s"$name.functions") = Prop.forAll { (fs: List[A => B], a: A) =>
      fs.map(_(a)).toSet.size > 1
    }
    property(s"$name.inputs") = Prop.forAll { (f: A => B, as: List[A]) =>
      as.map(f).toSet.size > 1
    }
  }

  test[Numbers, Numbers]

  {
    // Gen[A => B] depends on Gen[B] and may run out of size
    import magnolia.scalacheck.semiauto.ArbitraryDerivation.Fallback
    implicit val f: Fallback[Shape] = Fallback[Circle]
    test[Shape, Shape]
    test[Numbers, Shape]
  }

  test[Shape, Numbers]
}

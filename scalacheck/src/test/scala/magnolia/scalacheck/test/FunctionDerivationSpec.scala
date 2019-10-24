package magnolia.scalacheck.test

import magnolia.test.Simple._
import magnolia.test.ADT._
import magnolia.scalacheck.auto._
import magnolia.test._
import org.scalacheck._

import scala.reflect._

object FunctionDerivationSpec extends MagnoliaSpec("FunctionDerivation") {
  private def test[A: Arbitrary : ClassTag, B: ClassTag](implicit actual: Arbitrary[A => B]): Unit = {
    ensureSerializable(actual)
    val p = new Properties(s"${className[A]}.${className[B]}") {
      property("consistency") = Prop.forAll { (f: A => B, a: A) =>
        f(a) == f(a)
      }
      implicit def arbAs[T: Arbitrary] = Arbitrary(Gen.listOfN(100, Arbitrary.arbitrary[T]))
      property("functions") = Prop.forAll { (fs: List[A => B], a: A) =>
        fs.map(_(a)).toSet.size > 1
      }
      property("inputs") = Prop.forAll { (f: A => B, as: List[A]) =>
        as.map(f).toSet.size > 1
      }
    }
    include(p)
  }

  test[Numbers, Numbers]
  test[Shape, Shape]
  test[Numbers, Shape]
  test[Shape, Numbers]
}

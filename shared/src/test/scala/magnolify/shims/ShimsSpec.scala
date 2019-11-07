package magnolify.shims

import org.scalacheck._

import scala.reflect._

object ShimsSpec extends Properties("Shims") {
  private def test[C[_]](
    implicit ct: ClassTag[C[Int]],
    ti: C[Int] => Iterable[Int],
    fc: FactoryCompat[Int, C[Int]]
  ): Unit = {
    val name = ct.runtimeClass.getSimpleName
    property(name) = Prop.forAll { xs: List[Int] =>
      fc.build(xs).toList == xs
    }
  }

  test[Array]
  // Deprecated in 2.13
  // test[Traversable]
  test[Iterable]
  test[Seq]
  test[IndexedSeq]
  test[List]
  test[Vector]
  // Deprecated in 2.13
  // test[Stream]
}

package magnolify.shims

object ShimsTest {
  implicitly[Array[Int] => Seq[Int]]
  // implicitly[Traversable[Int] => Seq[Int]]
  // implicitly[Iterable[Int] => Seq[Int]]
  implicitly[Seq[Int] => Seq[Int]]
  implicitly[IndexedSeq[Int] => Seq[Int]]
  implicitly[List[Int] => Seq[Int]]
  implicitly[Vector[Int] => Seq[Int]]
  implicitly[Stream[Int] => Seq[Int]]

  implicitly[FactoryCompat[Int, Array[Int]]]
  implicitly[FactoryCompat[Int, Traversable[Int]]]
  implicitly[FactoryCompat[Int, Iterable[Int]]]
  implicitly[FactoryCompat[Int, Seq[Int]]]
  implicitly[FactoryCompat[Int, IndexedSeq[Int]]]
  implicitly[FactoryCompat[Int, List[Int]]]
  implicitly[FactoryCompat[Int, Vector[Int]]]
  implicitly[FactoryCompat[Int, Stream[Int]]]
}

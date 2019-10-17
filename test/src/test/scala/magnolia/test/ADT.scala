package magnolia.test

import org.scalacheck._

object ADT {
  sealed trait Node
  case class Leaf(value: Int) extends Node
  case class Branch(left: Node, right: Node) extends Node

  sealed trait GNode[+T]
  case class GLeaf[+T](value: T) extends GNode[T]
  case class GBranch[+T](left: GNode[T], right: GNode[T]) extends GNode[T]

  sealed trait Shape
  case object Space extends Shape
  case class Point(x: Int, y: Int) extends Shape
  case class Circle(r: Int) extends Shape

  sealed trait Color
  case object Red extends Color
  case object Green extends Color
  case object Blue extends Color

  // Keeping Gen[T] instances of recursive ADTs here to avoid implicit pollution from ArbDerivation
  // It seems that Magnolia SealedTrait#subtypes are ordered by name, so Gen.oneOf must match

  object Node {
    private val genLeaf = for {
      v <- Arbitrary.arbInt.arbitrary
    } yield Leaf(v)
    private def genBranch = for {
      l <- gen
      r <- gen
    } yield Branch(l, r)
    def gen: Gen[Node] = Gen.lzy(Gen.oneOf[Node](genBranch, genLeaf))
  }

  object GNode {
    implicit private def genLeaf[T](implicit arbT: Arbitrary[T]): Gen[GLeaf[T]] = for {
      v <- arbT.arbitrary
    } yield GLeaf[T](v)
    implicit private def genBranch[T](implicit arbT: Arbitrary[T]): Gen[GBranch[T]] = for {
      l <- gen
      r <- gen
    } yield GBranch(l, r)
    def gen[T: Arbitrary]: Gen[GNode[T]] =
      Gen.lzy(Gen.oneOf(genBranch, genLeaf))
  }
}

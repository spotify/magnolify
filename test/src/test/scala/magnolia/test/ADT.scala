package magnolia.test

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
}

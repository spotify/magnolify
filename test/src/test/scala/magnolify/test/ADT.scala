/*
 * Copyright 2019 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package magnolify.test

import magnolify.test.Simple.ScalaAnnotation

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

  @ScalaAnnotation("Color")
  sealed trait Color
  @ScalaAnnotation("Red")
  case object Red extends Color
  case object Green extends Color
  case object Blue extends Color

  // This is needed to simulate an error with "no valid constructor"
  // exception on attempt to deserialize a case object implementing an abstract class without
  // a no-arg constructor (observed on Scala 2.12 only).
  sealed abstract class Person(val entryName: String)
  case object Aldrin extends Person("Aldrin")
  case object Neil extends Person("Neil")

}

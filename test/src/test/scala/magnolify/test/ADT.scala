/*
 * Copyright 2019 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.test

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

object Product {
  case class Artist(artistId: Int, artistName: String)
  case class Track(
    trackId: Int,
    trackName: String,
    artistValue: Artist,
    genreList: List[String],
    expValue: Boolean,
    extra: Option[String]
  )
}

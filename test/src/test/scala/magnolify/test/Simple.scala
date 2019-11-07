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

import java.net.URI
import java.time.Duration

import org.scalacheck._
import cats._
import cats.instances.all._

object Simple {
  case class Integers(i: Int, l: Long)
  case class Numbers(i: Int, l: Long, f: Float, d: Double, bi: BigInt, bd: BigDecimal)
  case class Required(b: Boolean, i: Int, s: String)
  case class Nullable(b: Option[Boolean], i: Option[Int], s: Option[String])
  case class Repeated(b: List[Boolean], i: List[Int], s: List[String])
  case class Nested(
    b: Boolean,
    i: Int,
    s: String,
    r: Required,
    o: Option[Required],
    l: List[Required]
  )
  case class Collections(a: Array[Int], l: List[Int], v: Vector[Int])
  case class MoreCollections(i: Iterable[Int], s: Seq[Int], is: IndexedSeq[Int])
  case class Custom(u: URI, d: Duration)

  object Collections {
    implicit def eqIterable[T, C[T]](implicit eq: Eq[T], tt: C[T] => Iterable[T]): Eq[C[T]] =
      Eq.instance { (x, y) =>
        val xs = x.toList
        val ys = y.toList
        xs.size == ys.size && (x.iterator zip y.iterator).forall((eq.eqv _).tupled)
      }
  }

  object Custom {
    implicit val arbUri: Arbitrary[URI] =
      Arbitrary(Gen.alphaNumStr.map(URI.create))
    implicit val arbDuration: Arbitrary[Duration] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Duration.ofMillis(_)))
    implicit val coUri: Cogen[URI] = Cogen(_.toString.hashCode())
    implicit val coDuration: Cogen[Duration] = Cogen(_.toMillis)
    implicit val eqUri: Eq[URI] = Eq.by(_.toString)
    implicit val eqDuration: Eq[Duration] = Eq.by(_.toMillis)
  }
}

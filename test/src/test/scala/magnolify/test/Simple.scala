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

import scala.annotation.StaticAnnotation

object Simple {
  case class Integers(i: Int, l: Long)
  case class Floats(f: Float, d: Double)
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
  case class Enums(j: JavaEnums.Color, s: ScalaEnums.Color.Type, a: ADT.Color)
  case class Custom(u: URI, d: Duration)

  case class LowerCamel(firstField: String, secondField: String, innerField: LowerCamelInner)
  case class LowerCamelInner(innerFirst: String)

  object Collections {
    implicit def eqIterable[T, C[_]](implicit eq: Eq[T], tt: C[T] => Iterable[T]): Eq[C[T]] =
      Eq.instance { (x, y) =>
        val xs = x.toList
        val ys = y.toList
        xs.size == ys.size && (x.iterator zip y.iterator).forall((eq.eqv _).tupled)
      }
  }

  class ScalaAnnotation(val value: String) extends StaticAnnotation with Serializable

  object ScalaEnums {
    @JavaAnnotation("Java Annotation")
    @ScalaAnnotation("Scala Annotation")
    object Color extends Enumeration {
      type Type = Value
      val Red, Green, Blue = Value
    }
  }

  object Enums {
    implicit val arbScalaEnum: Arbitrary[ScalaEnums.Color.Type] =
      Arbitrary(Gen.oneOf(ScalaEnums.Color.values))
    implicit val eqJavaEnum: Eq[JavaEnums.Color] = Eq.by(_.name())
    implicit val eqScalaEnum: Eq[ScalaEnums.Color.Type] = Eq.by(_.toString)
  }

  object Custom {
    implicit val arbUri: Arbitrary[URI] =
      Arbitrary(Gen.alphaNumStr.map(URI.create))
    implicit val arbDuration: Arbitrary[Duration] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Duration.ofMillis(_)))
    implicit val coUri: Cogen[URI] = Cogen(_.toString.hashCode())
    implicit val coDuration: Cogen[Duration] = Cogen(_.toMillis)
    implicit val hashUri: Hash[URI] = Hash.fromUniversalHashCode[URI]
    implicit val hashDuration: Hash[Duration] = Hash.fromUniversalHashCode[Duration]

    implicit val showUri: Show[URI] = Show.fromToString
    implicit val showDuration: Show[Duration] = Show.fromToString
  }

  object LowerCamel {
    val fields: Seq[String] = Seq("firstField", "secondField", "innerField")
    val default: LowerCamel = LowerCamel("first", "second", LowerCamelInner("inner.first"))
  }
}

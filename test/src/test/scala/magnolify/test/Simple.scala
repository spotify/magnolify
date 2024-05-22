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

import java.net.URI
import java.time.Duration
import magnolify.shared.UnsafeEnum

import java.util
import java.util.Objects
import scala.annotation.StaticAnnotation
import scala.collection.immutable.Seq

object Simple {
  class ScalaAnnotation(val value: String) extends StaticAnnotation with Serializable

  object ScalaEnums {
    @JavaAnnotation("Java Annotation")
    @ScalaAnnotation("Scala Annotation")
    object Color extends Enumeration {
      type Type = Value
      val Red, Green, Blue = Value
    }
  }

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
  case class Collections(a: Array[Int], l: List[Int], v: Vector[Int], s: Set[Int]) {

    override def hashCode(): Int = {
      var hash = 1
      hash = 31 * hash + util.Arrays.hashCode(a)
      hash = 31 * hash + Objects.hashCode(l)
      hash = 31 * hash + Objects.hashCode(v)
      hash = 31 * hash + Objects.hashCode(s)
      hash
    }

    override def equals(obj: Any): Boolean = obj match {
      case that: Collections =>
        Objects.deepEquals(this.a, that.a) &&
        Objects.equals(this.l, that.l) &&
        Objects.equals(this.v, that.v) &&
        Objects.equals(this.s, that.s)
      case _ => false
    }
  }
  case class MoreCollections(i: Iterable[Int], s: Seq[Int], is: IndexedSeq[Int])
  case class Enums(
    j: JavaEnums.Color,
    s: ScalaEnums.Color.Type,
    a: ADT.Color,
    jo: Option[JavaEnums.Color],
    so: Option[ScalaEnums.Color.Type],
    ao: Option[ADT.Color],
    jr: List[JavaEnums.Color],
    sr: List[ScalaEnums.Color.Type],
    ar: List[ADT.Color]
  )
  case class UnsafeEnums(
    j: UnsafeEnum[JavaEnums.Color],
    s: UnsafeEnum[ScalaEnums.Color.Type],
    a: UnsafeEnum[ADT.Color],
    jo: Option[UnsafeEnum[JavaEnums.Color]],
    so: Option[UnsafeEnum[ScalaEnums.Color.Type]],
    ao: Option[UnsafeEnum[ADT.Color]],
    jr: List[UnsafeEnum[JavaEnums.Color]],
    sr: List[UnsafeEnum[ScalaEnums.Color.Type]],
    ar: List[UnsafeEnum[ADT.Color]]
  )
  case class Custom(u: URI, d: Duration)

  case class LowerCamel(firstField: String, secondField: String, innerField: LowerCamelInner)
  case class LowerCamelInner(innerFirst: String)

  object LowerCamel {
    val fields: Seq[String] = Seq("firstField", "secondField", "innerField")
    val selectedFields: Seq[String] = Seq("firstField", "secondField", "innerField.innerFirst")

    val default: LowerCamel = LowerCamel("first", "second", LowerCamelInner("inner.first"))
  }

  case class ValueClass(str: String) extends AnyVal
  case class HasValueClass(vc: ValueClass)
}

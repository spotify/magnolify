package magnolia.test

import java.net.URI
import java.time.Duration

import org.scalacheck._

object Simple {
  case class Integers(i: Int, l: Long)
  case class Numbers(i: Int, l: Long, f: Float, d: Double, bi: BigInt, bd: BigDecimal)
  case class Required(b: Boolean, i: Int, s: String)
  case class Nullable(b: Option[Boolean], i: Option[Int], s: Option[String])
  case class Repeated(b: List[Boolean], i: List[Int], s: List[String])
  case class Nested(b: Boolean, i: Int, s: String, r: Required)
  case class Collections(a: Array[Int], l: List[Int], v: Vector[Int])
  case class Custom(u: URI, d: Duration)

  object Custom {
    implicit val arbUri: Arbitrary[URI] =
      Arbitrary(Gen.alphaNumStr.map(URI.create))
    implicit val arbDuration: Arbitrary[Duration] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Duration.ofMillis(_)))
    implicit val coUri: Cogen[URI] = Cogen(_.toString.hashCode())
    implicit val coDuration: Cogen[Duration] = Cogen(_.toMillis)
  }
}

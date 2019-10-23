package magnolia.test

import com.google.protobuf.ByteString
import org.joda.time._
import org.scalacheck._

object Simple {
  case class Integers(i: Int, l: Long)
  case class Numbers(i: Int, l: Long, f: Float, d: Double, bi: BigInt, bd: BigDecimal)
  case class Required(b: Boolean, i: Int, s: String)
  case class Nullable(b: Option[Boolean], i: Option[Int], s: Option[String])
  case class Repeated(b: List[Boolean], i: List[Int], s: List[String])
  case class Nested(b: Boolean, i: Int, s: String, r: Required)
  case class Custom(b: ByteString, d: Duration)
  case class Timestamps(i: Instant, d: LocalDate, t: LocalTime, dt: LocalDateTime)

  object Custom {
    implicit val arbByteString: Arbitrary[ByteString] =
      Arbitrary(Gen.alphaNumStr.map(ByteString.copyFromUtf8))
    implicit val arbDuration: Arbitrary[Duration] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Duration.millis(_)))
    implicit val coByteString: Cogen[ByteString] = Cogen(_.hashCode())
    implicit val coDuration: Cogen[Duration] = Cogen(_.getMillis)
  }

  object Timestamps {
    implicit val arbInstant: Arbitrary[Instant] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Instant.ofEpochMilli(_)))
    implicit val arbDate: Arbitrary[LocalDate] =
      Arbitrary(arbInstant.arbitrary.map(i => new LocalDate(i.getMillis)))
    implicit val arbTime: Arbitrary[LocalTime] =
      Arbitrary(arbInstant.arbitrary.map(i => new LocalTime(i.getMillis)))
    implicit val arbDateTime: Arbitrary[LocalDateTime] =
      Arbitrary(arbInstant.arbitrary.map(i => new LocalDateTime(i.getMillis)))
  }
}

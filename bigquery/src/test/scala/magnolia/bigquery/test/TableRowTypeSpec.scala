package magnolia.bigquery.test

import java.net.URI
import java.time.{Duration => JDuration}

import cats._
import cats.instances.all._
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.api.services.bigquery.model.TableRow
import magnolia.bigquery._
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.Simple._
import magnolia.test._
import org.joda.time._
import org.scalacheck._

import scala.reflect._

object TableRowTypeSpec extends MagnoliaSpec("TableRowType") {
  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  private def test[T: Arbitrary : ClassTag](implicit tpe: TableRowType[T], eq: Eq[T]): Unit = {
    ensureSerializable(tpe)
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy1 = tpe(r)
      val copy2 = tpe(mapper.readValue(mapper.writeValueAsString(r), classOf[TableRow]))
      Prop.all(eq.eqv(t, copy1), eq.eqv(t, copy2))
    }
  }

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    implicit val eqArray: Eq[Array[Int]] = Eq.by(_.toList)
    test[Collections]
  }

  {
    import Custom._
    implicit val eqUri: Eq[URI] = Eq.by(_.toString)
    implicit val eqDuration: Eq[JDuration] = Eq.by(_.toMillis)
    implicit val trfUri: TableRowField[URI] = TableRowField[String].imap(URI.create)(_.toString)
    implicit val trfDuration: TableRowField[JDuration] =
      TableRowField[Long].imap(JDuration.ofMillis)(_.toMillis)
    test[Custom]
  }

  {
    implicit val arbInstant: Arbitrary[Instant] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Instant.ofEpochMilli(_)))
    implicit val arbDate: Arbitrary[LocalDate] =
      Arbitrary(arbInstant.arbitrary.map(i => new LocalDate(i.getMillis)))
    implicit val arbTime: Arbitrary[LocalTime] =
      Arbitrary(arbInstant.arbitrary.map(i => new LocalTime(i.getMillis)))
    implicit val arbDateTime: Arbitrary[LocalDateTime] =
      Arbitrary(arbInstant.arbitrary.map(i => new LocalDateTime(i.getMillis)))
    implicit val eqInstant: Eq[Instant] = Eq.by(_.getMillis)
    implicit val eqDate: Eq[LocalDate] = Eq.instance((x, y) => (x compareTo y) == 0)
    implicit val eqTime: Eq[LocalTime] = Eq.instance((x, y) => (x compareTo y) == 0)
    implicit val eqDateTime: Eq[LocalDateTime] = Eq.instance((x, y) => (x compareTo y) == 0)
    test[Timestamps]
  }
}

case class Timestamps(i: Instant, d: LocalDate, t: LocalTime, dt: LocalDateTime)

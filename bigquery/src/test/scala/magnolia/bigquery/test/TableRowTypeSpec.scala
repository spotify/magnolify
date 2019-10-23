package magnolia.bigquery.test

import cats._
import cats.instances.all._
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.api.services.bigquery.model.TableRow
import com.google.protobuf.ByteString
import magnolia.bigquery.auto._
import magnolia.cats.auto._
import magnolia.scalacheck.auto._
import magnolia.test.SerializableUtils
import magnolia.test.Simple._
import org.joda.time._
import org.scalacheck._

import scala.reflect._

object TableRowTypeSpec extends Properties("TableRowType") {
  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  private def test[T: Arbitrary : Eq : ClassTag](implicit tpe: TableRowType[T]): Unit = {
    SerializableUtils.ensureSerializable(tpe)
    val name = classTag[T].runtimeClass.getSimpleName
    val eq = implicitly[Eq[T]]
    property(s"$name") = Prop.forAll { t: T =>
      val tr = tpe(t)
      val copy1 = tpe(tr)
      val copy2 = tpe(mapper.readValue(mapper.writeValueAsString(tr), classOf[TableRow]))
      Prop.all(eq.eqv(t, copy1), eq.eqv(t, copy2))
    }
  }

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    import Custom._
    implicit val eqByteString: Eq[ByteString] = Eq.instance(_ == _)
    implicit val eqDuration: Eq[Duration] = Eq.by(_.getMillis)
    implicit val trtByteString: TableRowMappable[ByteString] =
      TableRowMappable[Array[Byte]].imap(ByteString.copyFrom)(_.toByteArray)
    implicit val trtDuration: TableRowMappable[Duration] =
      TableRowMappable[Long].imap(Duration.millis)(_.getMillis)
    test[Custom]
  }

  {
    import Timestamps._
    implicit val eqInstant: Eq[Instant] = Eq.by(_.getMillis)
    implicit val eqDate: Eq[LocalDate] = Eq.instance((x, y) => (x compareTo y) == 0)
    implicit val eqTime: Eq[LocalTime] = Eq.instance((x, y) => (x compareTo y) == 0)
    implicit val eqDateTime: Eq[LocalDateTime] = Eq.instance((x, y) => (x compareTo y) == 0)
    test[Timestamps]
  }
}

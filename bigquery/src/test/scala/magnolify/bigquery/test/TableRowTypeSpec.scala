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
package magnolify.bigquery.test

import java.net.URI
import java.{time => jt}

import cats._
import cats.instances.all._
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.api.services.bigquery.model.TableRow
import magnolify.bigquery._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import magnolify.test._
import org.joda.time._
import org.scalacheck._

import scala.reflect._

object TableRowTypeSpec extends MagnolifySpec("TableRowType") {
  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  private def test[T: Arbitrary: ClassTag](implicit tpe: TableRowType[T], eq: Eq[T]): Unit = {
    ensureSerializable(tpe)
    // FIXME: test schema
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy1 = tpe(r)
      val copy2 = tpe(mapper.readValue(mapper.writeValueAsString(r), classOf[TableRow]))
      Prop.all(eq.eqv(t, copy1), eq.eqv(t, copy2))
    }
  }

  implicit val trfInt: TableRowField[Int] = TableRowField.from[Long](_.toInt)(_.toLong)

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]

  {
    import Collections._
    test[Collections]
    test[MoreCollections]
  }

  {
    import Custom._
    implicit val trfUri: TableRowField[URI] = TableRowField.from[String](URI.create)(_.toString)
    implicit val trfDuration: TableRowField[jt.Duration] =
      TableRowField.from[Long](jt.Duration.ofMillis)(_.toMillis)
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
    implicit val arbBigDecimal: Arbitrary[BigDecimal] =
      Arbitrary(Gen.choose(0, Int.MaxValue).map(BigDecimal(_)))
    implicit val eqInstant: Eq[Instant] = Eq.by(_.getMillis)
    implicit val eqDate: Eq[LocalDate] = Eq.instance((x, y) => (x compareTo y) == 0)
    implicit val eqTime: Eq[LocalTime] = Eq.instance((x, y) => (x compareTo y) == 0)
    implicit val eqDateTime: Eq[LocalDateTime] = Eq.instance((x, y) => (x compareTo y) == 0)
    test[BigQueryTypes]
  }

  {
    implicit val efInt: TableRowField[Int] = TableRowField.from[Long](_.toInt)(_.toLong)
    implicit val efUri: TableRowField[URI] = TableRowField.from[String](URI.create)(_.toString)
  }
}

case class BigQueryTypes(i: Instant, d: LocalDate, t: LocalTime, dt: LocalDateTime, bd: BigDecimal)

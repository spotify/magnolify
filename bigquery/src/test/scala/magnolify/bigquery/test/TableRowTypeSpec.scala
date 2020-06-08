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
import magnolify.bigquery.unsafe._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shims.JavaConverters._
import magnolify.test.Simple._
import magnolify.test._
import org.joda.time._
import org.scalacheck._

import scala.reflect._

object TableRowTypeSpec extends MagnolifySpec("TableRowType") {
  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  private def test[T: Arbitrary: ClassTag](implicit t: TableRowType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    // FIXME: test schema
    tpe.schema
    property(className[T]) = Prop.forAll { t: T =>
      val r = tpe(t)
      val copy1 = tpe(r)
      val copy2 = tpe(mapper.readValue(mapper.writeValueAsString(r), classOf[TableRow]))
      Prop.all(eq.eqv(t, copy1), eq.eqv(t, copy2))
    }
  }

  private def roundTrip[T: Arbitrary: ClassTag](t: T)(implicit
    at: TableRowType[T],
    eqt: Eq[T]
  ): Unit =
    eqt.eqv(t, at.from(at.to(t)))

  test[Integers]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Unsafe]

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
    val trt = ensureSerializable(TableRowType[TableRowDesc])
    val schema = trt.schema
    require(trt.description == "TableRow with description")
    val fields = schema.getFields.asScala
    require(fields.find(_.getName == "s").exists(_.getDescription == "string"))
    require(fields.find(_.getName == "i").exists(_.getDescription == "integers"))
  }

  {
    val trt = ensureSerializable(TableRowType[CustomDesc])
    val schema = trt.schema
    require(trt.description == "my-project:my-dataset.my-table")
    val fields = schema.getFields.asScala
    require(
      fields
        .find(_.getName == "s")
        .exists(
          _.getDescription ==
            """{"description": "string", "since": "2020-01-01"}"""
        )
    )
    require(
      fields
        .find(_.getName == "i")
        .exists(
          _.getDescription ==
            """{"description": "integers", "since": "2020-02-01"}"""
        )
    )
  }

  require(
    expectException[IllegalArgumentException](TableRowType[DoubleRecordDesc]).getMessage ==
      "requirement failed: More than one @description annotation: magnolify.bigquery.test.DoubleRecordDesc"
  )
  require(
    expectException[IllegalArgumentException](TableRowType[DoubleFieldDesc]).getMessage ==
      "requirement failed: More than one @description annotation: magnolify.bigquery.test.DoubleFieldDesc#i"
  )

  {
    val it = TableRowType[DefaultInner]
    require(it(new TableRow) == DefaultInner())
    val inner = DefaultInner(2, Some(2), List(2, 2))
    require(it(it(inner)) == inner)

    val ot = TableRowType[DefaultOuter]
    require(ot(new TableRow) == DefaultOuter())
    val outer =
      DefaultOuter(DefaultInner(3, Some(3), List(3, 3)), Some(DefaultInner(3, Some(3), List(3, 3))))
    require(ot(ot(outer)) == outer)
  }

  {
    import magnolify.shared.CaseMapper._
    import magnolify.test.Product._
    implicit val trTrack = TableRowType[Track](toSnakeCase)
    test[Track]
    val track =
      Track(1, "Scala Scala", Artist(123, "Martin"), List("a", "b", "c"), true, Option(null))
    roundTrip(track)
  }
}

case class Unsafe(b: Byte, c: Char, s: Short, i: Int, _f: Float)
case class BigQueryTypes(i: Instant, d: LocalDate, t: LocalTime, dt: LocalDateTime, bd: BigDecimal)

@description("TableRow with description")
case class TableRowDesc(@description("string") s: String, @description("integers") i: Integers)

class tableDesc(projectId: String, datasetId: String, tableId: String)
    extends description(s"$projectId:$datasetId.$tableId")

class fieldDesc(description: String, since: LocalDate)
    extends description(
      s"""{"description": "$description", "since": "${since.toString("YYYY-MM-dd")}"}"""
    )

@tableDesc("my-project", "my-dataset", "my-table")
case class CustomDesc(
  @fieldDesc("string", LocalDate.parse("2020-01-01")) s: String,
  @fieldDesc("integers", LocalDate.parse("2020-02-01")) i: Integers
)

@description("desc1")
@description("desc2")
case class DoubleRecordDesc(i: Int)
case class DoubleFieldDesc(@description("desc1") @description("desc2") i: Int)

case class DefaultInner(i: Int = 1, o: Option[Int] = Some(1), l: List[Int] = List(1, 1))
case class DefaultOuter(
  i: DefaultInner = DefaultInner(2, Some(2), List(2, 2)),
  o: Option[DefaultInner] = Some(DefaultInner(2, Some(2), List(2, 2)))
)

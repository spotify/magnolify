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

package magnolify.bigquery.test

import java.net.URI
import java.time._
import java.time.format.DateTimeFormatter

import cats._
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.api.services.bigquery.model.TableRow
import magnolify.bigquery._
import magnolify.shared.CaseMapper
import magnolify.test.Simple._
import magnolify.test._
import org.scalacheck._

import scala.reflect._

import scala.jdk.CollectionConverters._

class TableRowTypeSuite
    extends MagnolifySuite
    with magnolify.scalacheck.AutoDerivation
    with magnolify.cats.AutoDerivation
    with magnolify.shared.AutoDerivation
    with magnolify.bigquery.AutoDerivation
    with magnolify.shared.EnumImplicits
    with magnolify.bigquery.BigQueryImplicits
    with magnolify.bigquery.unsafe.BigQueryUnsafeImplicits {

  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  private def test[T: Arbitrary: ClassTag: Eq: TableRowType]: Unit = {
    // val tpe = ensureSerializable(t)
    val tpe = implicitly[TableRowType[T]]
    val eq = implicitly[Eq[T]]
    // FIXME: test schema
    tpe.schema
    property(className[T]) {
      Prop.forAll { (t: T) =>
        val r = tpe(t)
        val copy1 = tpe(r)
        val copy2 = tpe(mapper.readValue(mapper.writeValueAsString(r), classOf[TableRow]))
        Prop.all(eq.eqv(t, copy1), eq.eqv(t, copy2))
      }
    }
  }

  import magnolify.scalacheck.test.TestArbitraryImplicits._
  import magnolify.cats.test.TestEqImplicits._
  // `TableRow` reserves field `f`
  implicit val trtFloats: TableRowType[Floats] =
    TableRowType[Floats](CaseMapper(s => if (s == "f") "float" else s))
  implicit val trfUri: TableRowField[URI] = TableRowField.from[String](URI.create)(_.toString)
  implicit val trfDuration: TableRowField[Duration] =
    TableRowField.from[Long](Duration.ofMillis)(_.toMillis)
  implicit val arbBigDecimal: Arbitrary[BigDecimal] =
    Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(BigDecimal(_)))

  test[Integers]
  test[Floats]
  test[Required]
  test[Nullable]
  test[Repeated]
  test[Nested]
  test[Unsafe]
  test[Collections]
  test[MoreCollections]
  test[Enums]
  test[UnsafeEnums]
  test[Custom]
  test[BigQueryTypes]

  test("BigDecimal") {
    val at: TableRowType[BigDec] = TableRowType[BigDec]
    val msg1 = "requirement failed: " +
      "Cannot encode BigDecimal 1234567890123456789012345678901234567890: precision 40 > 38"
    interceptMessage[IllegalArgumentException](msg1) {
      at(BigDec(BigDecimal("1234567890123456789012345678901234567890")))
    }
    val msg2 = "requirement failed: Cannot encode BigDecimal 3.14159265358979323846: scale 20 > 9"
    interceptMessage[IllegalArgumentException](msg2) {
      at(BigDec(BigDecimal("3.14159265358979323846")))
    }
  }

  test("TableRowDesc") {
//    val trt = ensureSerializable(TableRowType[TableRowDesc])
    val trt = TableRowType[TableRowDesc]
    val schema = trt.schema
    assertEquals(trt.description, "TableRow with description")
    val fields = schema.getFields.asScala
    assert(fields.find(_.getName == "s").exists(_.getDescription == "string"))
    assert(fields.find(_.getName == "i").exists(_.getDescription == "integers"))
  }

  test("CustomDesc") {
//    val trt = ensureSerializable(TableRowType[CustomDesc])
    val trt = TableRowType[CustomDesc]
    val schema = trt.schema
    assertEquals(trt.description, "my-project:my-dataset.my-table")
    val fields = schema.getFields.asScala
    val ds = """{"description": "string", "since": "2020-01-01"}"""
    val di = """{"description": "integers", "since": "2020-02-01"}"""
    assert(fields.find(_.getName == "s").exists(_.getDescription == ds))
    assert(fields.find(_.getName == "i").exists(_.getDescription == di))
  }

  testFail(TableRowType[DoubleRecordDesc])(
    "More than one @description annotation: magnolify.bigquery.test.DoubleRecordDesc"
  )
  testFail(TableRowType[DoubleFieldDesc])(
    "More than one @description annotation: magnolify.bigquery.test.DoubleFieldDesc#i"
  )

  test("DefaultInner") {
    // val trt = ensureSerializable(TableRowType[DefaultInner])
    val trt = TableRowType[DefaultInner]
    assertEquals(trt(new TableRow), DefaultInner())
    val inner = DefaultInner(2, Some(2), List(2, 2))
    assertEquals(trt(trt(inner)), inner)
  }

  test("DefaultOuter") {
    // val trt = ensureSerializable(TableRowType[DefaultOuter])
    val trt = TableRowType[DefaultOuter]
    assertEquals(trt(new TableRow), DefaultOuter())
    val outer =
      DefaultOuter(DefaultInner(3, Some(3), List(3, 3)), Some(DefaultInner(3, Some(3), List(3, 3))))
    assertEquals(trt(trt(outer)), outer)
  }

  {
    implicit val trt: TableRowType[LowerCamel] = TableRowType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    test("LowerCamel mapping") {
      val schema = trt.schema
      val fields = LowerCamel.fields.map(_.toUpperCase)
      assertEquals(schema.getFields.asScala.map(_.getName).toSeq, fields)
      assert(
        schema.getFields.asScala
          .find(_.getName == "INNERFIELD")
          .exists(_.getFields.asScala.exists(_.getName == "INNERFIRST"))
      )

      val record = trt(LowerCamel.default)
      assertEquals(record.keySet().asScala.toSet, fields.toSet)
      assert(record.get("INNERFIELD").asInstanceOf[TableRow].get("INNERFIRST") != null)
    }
  }
}

case class Unsafe(b: Byte, c: Char, s: Short, i: Int, _f: Float)
case class BigQueryTypes(i: Instant, d: LocalDate, t: LocalTime, dt: LocalDateTime, bd: BigDecimal)
case class BigDec(bd: BigDecimal)

@description("TableRow with description")
case class TableRowDesc(@description("string") s: String, @description("integers") i: Integers)

class tableDesc(projectId: String, datasetId: String, tableId: String)
    extends description(s"$projectId:$datasetId.$tableId")

class fieldDesc(description: String, since: LocalDate)
    extends description(
      s"""{"description": "$description", "since": "${since.format(
          DateTimeFormatter.ofPattern("YYYY-MM-dd")
        )}"}"""
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

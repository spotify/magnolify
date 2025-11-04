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

package magnolify.bigquery

import cats._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.api.services.bigquery.model.TableRow
import magnolify.bigquery._
import magnolify.bigquery.unsafe._
import magnolify.cats.auto._
import magnolify.cats.TestEq._
import magnolify.scalacheck.auto._
import magnolify.scalacheck.TestArbitrary._
import magnolify.shared.CaseMapper
import magnolify.shared.TestEnumType._
import magnolify.test.Simple._
import magnolify.test._
import org.joda.time as joda
import org.scalacheck._

import java.net.URI
import java.time._
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters._
import scala.reflect._

class TableRowTypeSuite extends MagnolifySuite {
  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  private def test[T: Arbitrary: ClassTag](implicit t: TableRowType[T], eq: Eq[T]): Unit = {
    val tpe = ensureSerializable(t)
    // FIXME: test schema
    val _ = tpe.schema
    property(className[T]) {
      Prop.forAll { (t: T) =>
        val r = tpe(t)
        val copy1 = tpe(r)
        val copy2 = tpe(mapper.readValue(mapper.writeValueAsString(r), classOf[TableRow]))
        Prop.all(eq.eqv(t, copy1), eq.eqv(t, copy2))
      }
    }
  }

  implicit val arbBigDecimal: Arbitrary[BigDecimal] =
    Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(BigDecimal(_)))
  implicit val arbBigNumeric: Arbitrary[BigNumeric] = Arbitrary(
    arbBigDecimal.arbitrary.map(BigNumeric(_))
  )
  // `TableRow` reserves field `f`
  implicit val trtFloats: TableRowType[Floats] =
    TableRowType[Floats](CaseMapper(s => if (s == "f") "float" else s))
  implicit val trfUri: TableRowField[URI] = TableRowField.from[String](URI.create)(_.toString)
  implicit val trfDuration: TableRowField[Duration] =
    TableRowField.from[Long](Duration.ofMillis)(_.toMillis)

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
  test[BigQueryJoda]

  test("AnyVal") {
    implicit val trt: TableRowType[HasValueClass] = TableRowType[HasValueClass]
    test[HasValueClass]

    assert(trt.schema.getFields.asScala.head.getType == "STRING")

    val record = trt(HasValueClass(ValueClass("String")))
    assert(record.get("vc") == "String")
  }

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

  test("BigNumeric") {
    val at: TableRowType[BigNum] = TableRowType[BigNum]
    val largePrecision = "9" * 80
    val msg1 = s"requirement failed: Cannot encode BigNumeric $largePrecision: precision 80 > 77"
    interceptMessage[IllegalArgumentException](msg1) {
      at(BigNum(BigNumeric(largePrecision)))
    }
    val largeScale = "1." + ("3" * 39)
    val msg2 = s"requirement failed: Cannot encode BigNumeric $largeScale: scale 39 > 38"
    interceptMessage[IllegalArgumentException](msg2) {
      at(BigNum(BigNumeric(largeScale)))
    }
  }

  test("TableRowDesc") {
    val trt = ensureSerializable(TableRowType[TableRowDesc])
    val schema = trt.schema
    assertEquals(trt.description, "TableRow with description")
    val fields = schema.getFields.asScala
    assert(fields.find(_.getName == "s").exists(_.getDescription == "string"))
    assert(fields.find(_.getName == "i").exists(_.getDescription == "integers"))
  }

  test("CustomDesc") {
    val trt = ensureSerializable(TableRowType[CustomDesc])
    val schema = trt.schema
    assertEquals(trt.description, "my-project:my-dataset.my-table")
    val fields = schema.getFields.asScala
    val ds = """{"description": "string", "since": "2020-01-01"}"""
    val di = """{"description": "integers", "since": "2020-02-01"}"""
    assert(fields.find(_.getName == "s").exists(_.getDescription == ds))
    assert(fields.find(_.getName == "i").exists(_.getDescription == di))
  }

  testFail(TableRowType[DoubleRecordDesc])(
    "More than one @description annotation: magnolify.bigquery.DoubleRecordDesc"
  )
  testFail(TableRowType[DoubleFieldDesc])(
    "More than one @description annotation: magnolify.bigquery.DoubleFieldDesc#i"
  )

  test("DefaultInner") {
    val trt = ensureSerializable(TableRowType[DefaultInner])
    assertEquals(trt(new TableRow), DefaultInner())
    val inner = DefaultInner(2, Some(2), List(2, 2))
    assertEquals(trt(trt(inner)), inner)
  }

  test("DefaultOuter") {
    val trt = ensureSerializable(TableRowType[DefaultOuter])
    assertEquals(trt(new TableRow), DefaultOuter())
    val outer =
      DefaultOuter(DefaultInner(3, Some(3), List(3, 3)), Some(DefaultInner(3, Some(3), List(3, 3))))
    assertEquals(trt(trt(outer)), outer)
  }

  {
    implicit val trt = TableRowType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    test("LowerCamel mapping") {
      val schema = trt.schema
      val fields = LowerCamel.fields.map(_.toUpperCase)
      assertEquals(schema.getFields.asScala.map(_.getName).toSeq, fields)
      val selectedFields = LowerCamel.selectedFields.map(_.toUpperCase)
      assertEquals(trt.selectedFields, selectedFields)
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

  {
    implicit val decoratedIntType: TableRowField[DecoratedInt] =
      TableRowField.xmap[Int, DecoratedInt](DecoratedInt(_), _.i)
    test[RecordWithDecoratedInt]

    val trt = TableRowType[RecordWithDecoratedInt]
    // Assert that schema uses the primitive int type, not the nested record type
    assert(trt.schema.getFields.get(0).getType == "INT64")
    assert(trt.schema.getFields.get(0).getName == "d")
  }
}

case class Unsafe(b: Byte, c: Char, s: Short, i: Int, _f: Float)
case class BigQueryTypes(
  i: Instant,
  d: LocalDate,
  t: LocalTime,
  dt: LocalDateTime,
  bd: BigDecimal,
  bn: BigNumeric
)
case class BigQueryJoda(
  i: joda.Instant,
  d: joda.LocalDate,
  t: joda.LocalTime,
  dt: joda.LocalDateTime
)
case class BigDec(bd: BigDecimal)
case class BigNum(bn: BigNumeric)

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

case class DecoratedInt(i: Int)
case class RecordWithDecoratedInt(d: DecoratedInt)

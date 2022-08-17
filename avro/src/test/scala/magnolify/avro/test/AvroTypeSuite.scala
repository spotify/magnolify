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

package magnolify.avro.test

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.nio.ByteBuffer
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, LocalDate, LocalDateTime, LocalTime}
import java.util.UUID
import cats._
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import magnolify.avro._
import magnolify.avro.unsafe._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shared.CaseMapper
import magnolify.test.Simple._
import magnolify.test.Time._
import magnolify.test._
import org.apache.avro.Schema
import org.apache.avro.generic.{
  GenericDatumReader,
  GenericDatumWriter,
  GenericRecord,
  GenericRecordBuilder
}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.scalacheck._

import scala.reflect._
import scala.util.Try
import scala.jdk.CollectionConverters._

class AvroTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit
    t: AvroType[T],
    eqt: Eq[T],
    eqr: Eq[GenericRecord] = Eq.instance(_ == _)
  ): Unit = {
    val tpe = ensureSerializable(t)
    // FIXME: test schema
    val copier = new Copier(tpe.schema)
    property(className[T]) {
      Prop.forAll { t: T =>
        val r = tpe(t)
        val rCopy = copier(r)
        val copy = tpe(rCopy)
        Prop.all(eqt.eqv(t, copy), eqr.eqv(r, rCopy))
      }
    }
  }

  private def assertLogicalType(
    schema: Schema,
    fieldName: String,
    logicalType: String,
    backwardsCompatible: Boolean = true
  ): Unit = {
    // validate that the string schema contains the correct logical type
    val jf = new JsonFactory
    val om = new ObjectMapper(jf)
    val tree: JsonNode = om.readTree(jf.createParser(schema.toString()))
    val sf = tree.get("fields").elements().asScala.find(_.get("name").asText() == fieldName)
    assert(sf.isDefined, s"schema field $fieldName is undefined")
    val slt = sf.flatMap(f => Try(f.get("type").get("logicalType").asText()).toOption)
    assert(
      slt.contains(logicalType),
      s"schema field $fieldName has logicalType ${slt.getOrElse("null")}, should be $logicalType"
    )

    // validate that the parsed schema contains the correct logical type if this type is backwards compatible or if we're testing with a recent avro
    val compatibilityMode = sys.props.get("avro.version").contains("1.8.2")
    val shouldTestSchema = backwardsCompatible || (!compatibilityMode)
    if (shouldTestSchema) {
      val f = schema.getFields.asScala.find(_.name() == fieldName)
      assert(f.isDefined, s"field $fieldName is undefined")
      val lt = f.flatMap(f => Try(f.schema().getLogicalType.getName).toOption)
      assert(
        lt.contains(logicalType),
        s"field $fieldName has logicalType ${lt.getOrElse("null")}, should be $logicalType"
      )
    }
  }

  test[Integers]
  test[Floats]
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
    import Enums._
    import UnsafeEnums._
    test[Enums]
    test[UnsafeEnums]
  }

  {
    import Custom._
    implicit val afUri: AvroField[URI] = AvroField.from[String](URI.create)(_.toString)
    implicit val afDuration: AvroField[Duration] =
      AvroField.from[Long](Duration.ofMillis)(_.toMillis)
    test[Custom]
  }

  {
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[AvroTypes]
  }

  {
    def f[T](r: GenericRecord): List[(String, Any)] =
      r.get("m")
        .asInstanceOf[java.util.Map[CharSequence, Any]]
        .asScala
        .toList
        .map(kv => (kv._1.toString, kv._2))
        .sortBy(_._1)
    implicit val eqMapPrimitive: Eq[GenericRecord] = Eq.instance((x, y) => f(x) == f(y))
    test[MapPrimitive]
    test[MapNested]
  }

  test[Logical]

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] =
      Arbitrary(Gen.chooseNum(0L, Long.MaxValue).map(BigDecimal(_, 0)))

    {
      import magnolify.avro.logical.micros._
      implicit val afBigDecimal: AvroField[BigDecimal] = AvroField.bigDecimal(19, 0)
      test[LogicalMicros]
    }

    test("MicrosLogicalTypes") {
      import magnolify.avro.logical.micros._
      implicit val afBigDecimal: AvroField[BigDecimal] = AvroField.bigDecimal(19, 0)

      val schema = AvroType[LogicalMicros].schema
      assertLogicalType(schema, "bd", "decimal")
      assertLogicalType(schema, "i", "timestamp-micros")
      assertLogicalType(schema, "dt", "local-timestamp-micros", false)
      assertLogicalType(schema, "t", "time-micros")
    }

    {
      import magnolify.avro.logical.millis._
      implicit val afBigDecimal: AvroField[BigDecimal] = AvroField.bigDecimal(19, 0)
      test[LogicalMillis]
    }

    test("MilliLogicalTypes") {
      import magnolify.avro.logical.millis._
      implicit val afBigDecimal: AvroField[BigDecimal] = AvroField.bigDecimal(19, 0)

      val schema = AvroType[LogicalMillis].schema
      assertLogicalType(schema, "bd", "decimal")
      assertLogicalType(schema, "i", "timestamp-millis")
      assertLogicalType(schema, "dt", "local-timestamp-millis", false)
      assertLogicalType(schema, "t", "time-millis")
    }

    {
      import magnolify.avro.logical.bigquery._
      test[LogicalBigQuery]
    }

    test("BigQueryLogicalTypes") {
      import magnolify.avro.logical.bigquery._
      // `registerLogicalTypes()` is necessary to correctly parse a custom logical type from string.
      // if omitted, the returned string schema will be correct, but the logicalType field will be null.
      // this is unfortunately global mutable state
      registerLogicalTypes()

      val schema = AvroType[LogicalBigQuery].schema
      assertLogicalType(schema, "bd", "decimal")
      assertLogicalType(schema, "i", "timestamp-micros")
      assertLogicalType(schema, "dt", "datetime")
      assertLogicalType(schema, "t", "time-micros")
    }
  }

  {
    implicit val arbCountryCode: Arbitrary[CountryCode] = Arbitrary(
      Gen.oneOf("US", "UK", "CA", "MX").map(CountryCode(_))
    )
    implicit val afCountryCode: AvroField[CountryCode] =
      AvroField.fixed[CountryCode](2)(bs => CountryCode(new String(bs)))(cc => cc.code.getBytes)
    test[Fixed]

    test("FixedDoc") {
      val at = ensureSerializable(AvroType[Fixed])
      val schema = at.schema.getField("countryCode").schema
      assertEquals(schema.getName, "CountryCode")
      assertEquals(schema.getNamespace, "magnolify.avro.test")
      assertEquals(schema.getDoc, "Fixed with doc")
    }
  }

  test("AvroDoc") {
    val at = ensureSerializable(AvroType[AvroDoc])
    val schema = at.schema
    assertEquals(schema.getDoc, "Avro with doc")
    val fields = schema.getFields.asScala
    assert(fields.find(_.name() == "s").exists(_.doc() == "string"))
    assert(fields.find(_.name() == "i").exists(_.doc() == "integers"))
  }

  test("CustomDoc") {
    val at = ensureSerializable(AvroType[CustomDoc])
    val schema = at.schema
    assertEquals(schema.getDoc, """{"doc": "Avro with doc", "path": "/path/to/my/data"}""")
    val fields = schema.getFields.asScala
    val ds = """{"doc": "string", "since": "2020-01-01"}"""
    val di = """{"doc": "integers", "since": "2020-02-01"}"""
    assert(fields.find(_.name() == "s").exists(_.doc() == ds))
    assert(fields.find(_.name() == "i").exists(_.doc() == di))
  }

  testFail(AvroType[DoubleRecordDoc])(
    "More than one @doc annotation: magnolify.avro.test.DoubleRecordDoc"
  )
  testFail(AvroType[DoubleFieldDoc])(
    "More than one @doc annotation: magnolify.avro.test.DoubleFieldDoc#i"
  )

  test("EnumDoc") {
    val at = ensureSerializable(AvroType[EnumDoc])
    assertEquals(at.schema.getField("p").schema().getDoc, "Avro enum")
  }

  test("DefaultInner") {
    import magnolify.avro.logical.bigquery._
    val at = ensureSerializable(AvroType[DefaultInner])
    assertEquals(at(new GenericRecordBuilder(at.schema).build()), DefaultInner())
    val inner = DefaultInner(
      2,
      Some(2),
      List(2, 2),
      Map("b" -> 2),
      JavaEnums.Color.GREEN,
      ScalaEnums.Color.Green,
      BigDecimal(222.222),
      UUID.fromString("22223333-abcd-abcd-abcd-222233334444"),
      Instant.ofEpochSecond(22334455L),
      LocalDate.ofEpochDay(2233),
      LocalTime.of(2, 3, 4),
      LocalDateTime.of(2002, 3, 4, 5, 6, 7)
    )
    assertEquals(at(at(inner)), inner)
  }

  test("DefaultOuter") {
    import magnolify.avro.logical.bigquery._
    val at = ensureSerializable(AvroType[DefaultOuter])
    assertEquals(at(new GenericRecordBuilder(at.schema).build()), DefaultOuter())
    val inner = DefaultInner(
      3,
      Some(3),
      List(3, 3),
      Map("c" -> 3),
      JavaEnums.Color.BLUE,
      ScalaEnums.Color.Blue,
      BigDecimal(333.333),
      UUID.fromString("33334444-abcd-abcd-abcd-333344445555"),
      Instant.ofEpochSecond(33445566L),
      LocalDate.ofEpochDay(3344),
      LocalTime.of(3, 4, 5),
      LocalDateTime.of(2003, 4, 5, 6, 7, 8)
    )
    val outer = DefaultOuter(inner, Some(inner))
    assertEquals(at(at(outer)), outer)
  }

  testFail(AvroType[SomeDefault])(
    "Option[T] can only default to None"
  )

  {
    implicit val at: AvroType[LowerCamel] = AvroType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    test("LowerCamel mapping") {
      val schema = at.schema
      val fields = LowerCamel.fields.map(_.toUpperCase)
      assertEquals(schema.getFields.asScala.map(_.name()).toSeq, fields)
      assertEquals(
        schema.getField("INNERFIELD").schema().getFields.asScala.map(_.name()).toSeq,
        Seq("INNERFIRST")
      )

      val record = at(LowerCamel.default)
      assert(fields.forall(record.get(_) != null))
      assert(record.get("INNERFIELD").asInstanceOf[GenericRecord].get("INNERFIRST") != null)
    }
  }

  test("BigDecimal") {
    import magnolify.avro.logical.bigquery._
    val at: AvroType[BigDec] = AvroType[BigDec]

    val bigInt = "1234567890123456789012345678901234567890"
    val msg1 = "requirement failed: " +
      s"Cannot encode BigDecimal $bigInt: precision 49 > 38 after set scale from 0 to 9"
    interceptMessage[IllegalArgumentException](msg1) {
      at(BigDec(BigDecimal(bigInt)))
    }

    val msg2 = "requirement failed: " +
      s"Cannot decode BigDecimal ${BigDecimal(BigInt(bigInt), 9)}: precision 40 > 38"
    val record = new GenericRecordBuilder(at.schema)
      .set("bd", ByteBuffer.wrap(BigDecimal(bigInt).underlying().unscaledValue().toByteArray))
      .build()
    interceptMessage[IllegalArgumentException](msg2) {
      at(record)
    }

    interceptMessage[ArithmeticException]("Rounding necessary") {
      at(BigDec(BigDecimal("3.14159265358979323846")))
    }
  }
}

case class Unsafe(b: Byte, c: Char, s: Short)
case class AvroTypes(ba: Array[Byte], u: Unit)
case class MapPrimitive(m: Map[String, Int])
case class MapNested(m: Map[String, Nested])

case class Logical(u: UUID, d: LocalDate)
case class LogicalMicros(bd: BigDecimal, i: Instant, t: LocalTime, dt: LocalDateTime)
case class LogicalMillis(bd: BigDecimal, i: Instant, t: LocalTime, dt: LocalDateTime)
case class LogicalBigQuery(bd: BigDecimal, i: Instant, t: LocalTime, dt: LocalDateTime)
case class BigDec(bd: BigDecimal)

@doc("Fixed with doc")
case class CountryCode(code: String)
case class Fixed(countryCode: CountryCode)

@doc("Avro with doc")
case class AvroDoc(@doc("string") s: String, @doc("integers") i: Integers)

class datasetDoc(doc: String, path: String) extends doc(s"""{"doc": "$doc", "path": "$path"}""")

class fieldDoc(doc: String, since: LocalDate)
    extends doc(
      s"""{"doc": "$doc", "since": "${since.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))}"}"""
    )

@datasetDoc("Avro with doc", "/path/to/my/data")
case class CustomDoc(
  @fieldDoc("string", LocalDate.of(2020, 1, 1)) s: String,
  @fieldDoc("integers", LocalDate.of(2020, 2, 1)) i: Integers
)

@doc("doc1")
@doc("doc2")
case class DoubleRecordDoc(i: Int)
case class DoubleFieldDoc(@doc("doc1") @doc("doc2") i: Int)

case class DefaultInner(
  i: Int = 1,
  o: Option[Int] = None,
  l: List[Int] = List(1, 1),
  m: Map[String, Int] = Map("a" -> 1),
  je: JavaEnums.Color = JavaEnums.Color.RED,
  se: ScalaEnums.Color.Type = ScalaEnums.Color.Red,
  bd: BigDecimal = BigDecimal(111.111),
  u: UUID = UUID.fromString("11112222-abcd-abcd-abcd-111122223333"),
  ts: Instant = Instant.ofEpochSecond(11223344),
  d: LocalDate = LocalDate.ofEpochDay(1122),
  t: LocalTime = LocalTime.of(1, 2, 3),
  dt: LocalDateTime = LocalDateTime.of(2001, 2, 3, 4, 5, 6)
)
case class DefaultOuter(
  i: DefaultInner = DefaultInner(
    2,
    None,
    List(2, 2),
    Map("b" -> 2),
    JavaEnums.Color.GREEN,
    ScalaEnums.Color.Green,
    BigDecimal(222.222),
    UUID.fromString("22223333-abcd-abcd-abcd-222233334444"),
    Instant.ofEpochSecond(22334455L),
    LocalDate.ofEpochDay(2233),
    LocalTime.of(2, 3, 4),
    LocalDateTime.of(2002, 3, 4, 5, 6, 7)
  ),
  o: Option[DefaultInner] = None
)
case class SomeDefault(o: Option[Int] = Some(1))

@doc("Avro enum")
object Pet extends Enumeration {
  type Type = Value
  val Cat, Dog = Value
}
case class EnumDoc(p: Pet.Type)

private class Copier(private val schema: Schema) {
  private val encoder = EncoderFactory.get
  private val decoder = DecoderFactory.get

  private val datumWriter = new GenericDatumWriter[GenericRecord](schema)
  private val datumReader = new GenericDatumReader[GenericRecord](schema)

  def apply(r: GenericRecord): GenericRecord = {
    val baos = new ByteArrayOutputStream()
    datumWriter.write(r, encoder.directBinaryEncoder(baos, null))
    datumReader.read(
      null,
      decoder.directBinaryDecoder(new ByteArrayInputStream(baos.toByteArray), null)
    )
  }
}

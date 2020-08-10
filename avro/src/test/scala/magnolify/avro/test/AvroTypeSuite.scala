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
package magnolify.avro.test

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.nio.ByteBuffer
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.UUID

import cats._
import cats.instances.all._
import magnolify.avro._
import magnolify.avro.unsafe._
import magnolify.cats.auto._
import magnolify.scalacheck.auto._
import magnolify.shared.CaseMapper
import magnolify.shims.JavaConverters._
import magnolify.test.Simple._
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
    test[Enums]
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

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] =
      Arbitrary(Gen.chooseNum(0L, Long.MaxValue).map(BigDecimal(_, 0)))
    implicit val arbInstant: Arbitrary[Instant] =
      Arbitrary(Gen.chooseNum(0, Int.MaxValue).map(Instant.ofEpochMilli(_)))
    implicit val arbDate: Arbitrary[LocalDate] =
      Arbitrary(arbInstant.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalDate))
    implicit val arbTime: Arbitrary[LocalTime] =
      Arbitrary(arbInstant.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalTime))
    implicit val arbDateTime: Arbitrary[LocalDateTime] =
      Arbitrary(arbInstant.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalDateTime))
    implicit val eqInstant: Eq[Instant] = Eq.by(_.toEpochMilli)
    implicit val eqDate: Eq[LocalDate] = Eq.by(_.toEpochDay)
    implicit val eqTime: Eq[LocalTime] = Eq.by(_.toNanoOfDay)
    implicit val eqDateTime: Eq[LocalDateTime] = Eq.by(_.toEpochSecond(ZoneOffset.UTC))

    {
      import magnolify.avro.logical.micros._
      implicit val afBigDecimal: AvroField[BigDecimal] = AvroField.bigDecimal(19, 0)
      test[Logical1]
    }

    {
      import magnolify.avro.logical.millis._
      implicit val afBigDecimal: AvroField[BigDecimal] = AvroField.bigDecimal(19, 0)
      test[Logical2]
    }

    {
      import magnolify.avro.logical.bigquery._
      test[BigQuery]
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
case class AvroTypes(bs: Array[Byte], u: Unit)
case class MapPrimitive(m: Map[String, Int])
case class MapNested(m: Map[String, Nested])

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

@doc("Avro enum")
object Pet extends Enumeration {
  type Type = Value
  val Cat, Dog = Value
}
case class EnumDoc(p: Pet.Type)

case class Logical1(
  bd: BigDecimal,
  u: UUID,
  i: Instant,
  d: LocalDate,
  t: LocalTime,
  dt: LocalDateTime
)
case class Logical2(
  bd: BigDecimal,
  u: UUID,
  i: Instant,
  d: LocalDate,
  t: LocalTime,
  dt: LocalDateTime
)
case class BigQuery(
  bd: BigDecimal,
  u: UUID,
  i: Instant,
  d: LocalDate,
  t: LocalTime,
  dt: LocalDateTime
)
case class BigDec(bd: BigDecimal)

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

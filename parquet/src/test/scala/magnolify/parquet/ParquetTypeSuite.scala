/*
 * Copyright 2022 Spotify AB
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

package magnolify.parquet

import cats._
import magnolify.cats.auto._
import magnolify.cats.TestEq._
import magnolify.parquet.unsafe._
import magnolify.scalacheck.auto._
import magnolify.scalacheck.TestArbitrary._
import magnolify.shared.CaseMapper
import magnolify.shared.doc
import magnolify.shared.TestEnumType._
import magnolify.test.Simple._
import magnolify.test._
import org.apache.parquet.io._
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.scalacheck._

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time._
import java.util.UUID
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

class ParquetTypeSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit
    t: ParquetType[T],
    eq: Eq[T]
  ): Unit = {
    // Ensure serializable even after evaluation of `schema`
    t.schema: Unit
    val tpe = ensureSerializable(t)

    property(className[T]) {
      Prop.forAll { (t: T) =>
        val out = new TestOutputFile
        val writer = tpe.writeBuilder(out).build()
        writer.write(t)
        writer.close()

        val in = new TestInputFile(out.getBytes)
        val reader = tpe.readBuilder(in).build()
        val copy = reader.read()
        val next = reader.read()
        reader.close()
        Prop.all(eq.eqv(t, copy), next == null)
      }
    }
  }

  implicit val pfUri: ParquetField[URI] =
    ParquetField.from[String](URI.create)(_.toString)
  implicit val pfDuration: ParquetField[Duration] =
    ParquetField.from[Long](Duration.ofMillis)(_.toMillis)

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

  test[ParquetTypes]

  test[MapPrimitive]
  test[MapNested]

  test("AnyVal") {
    implicit val pt: ParquetType[HasValueClass] = ParquetType[HasValueClass]
    test[HasValueClass]

    val schema = pt.schema
    val index = schema.getFieldIndex("vc")
    val field = schema.getFields.get(index)
    assert(field.isPrimitive)
    assert(field.asPrimitiveType().getPrimitiveTypeName == PrimitiveTypeName.BINARY)
  }

  test("ParquetDoc") {
    ensureSerializable(ParquetType[ParquetNestedDoc])
    val pf = ParquetField[ParquetNestedDoc]
    assert(pf.typeDoc.contains("Parquet with doc"))

    val fieldDocs = pf.fieldDocs(CaseMapper.identity)
    assert(fieldDocs("pd") == "nested")
    assert(fieldDocs("pd.i") == "integers")
    assert(fieldDocs("pd.s") == "string")
    assert(fieldDocs("i") == "integers")

    val upperFieldDocs = pf.fieldDocs(CaseMapper(_.toUpperCase()))
    assert(upperFieldDocs("PD") == "nested")
    assert(upperFieldDocs("PD.I") == "integers")
    assert(upperFieldDocs("PD.S") == "string")
    assert(upperFieldDocs("I") == "integers")
  }

  test("ParquetDocWithNestedList") {
    ensureSerializable(ParquetType[ParquetNestedListDoc])
    val pf = ParquetField[ParquetNestedListDoc]
    assert(pf.typeDoc.contains("Parquet with doc with nested list"))

    val fieldDocs = pf.fieldDocs(CaseMapper.identity)
    assert(fieldDocs("pd") == "nested")
    assert(fieldDocs("pd.i") == "integers")
    assert(fieldDocs("pd.s") == "string")
    assert(fieldDocs("i") == "integers")
  }

  // Precision = number of digits, so 5 means -99999 to 99999
  private def decimal(precision: Int): Arbitrary[BigDecimal] = {
    val max = BigInt(10).pow(precision) - 1
    Arbitrary(Gen.choose(-max, max).map(BigDecimal.apply))
  }

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] = decimal(9)
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimal32(9, 0)
    test[Decimal]
  }

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] = decimal(18)
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimal64(18, 0)
    test[Decimal]
  }

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] = decimal(18)
    // math.floor(math.log10(math.pow(2, 8*8-1) - 1)) = 18 digits
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimalFixed(8, 18, 0)
    test[Decimal]
  }

  {
    implicit val arbBigDecimal: Arbitrary[BigDecimal] = decimal(20)
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimalBinary(20, 0)
    test[Decimal]
  }

  test[Logical]

  property("Millis") {
    import magnolify.parquet.logical.millis._
    test[Time]
  }

  property("Micros") {
    import magnolify.parquet.logical.micros._
    test[Time]
  }

  property("Nanos") {
    import magnolify.parquet.logical.nanos._
    test[Time]
  }

  {
    implicit val pt: ParquetType[LowerCamel] = ParquetType[LowerCamel](CaseMapper(_.toUpperCase))
    test[LowerCamel]

    test("LowerCamel mapping") {
      val schema = pt.schema.asGroupType()
      val fields = LowerCamel.fields.map(_.toUpperCase)
      assertEquals(schema.getFields.asScala.map(_.getName).toSeq, fields)
      val inner = schema.getFields.asScala.find(_.getName == "INNERFIELD").get.asGroupType()
      assertEquals(inner.getFields.asScala.map(_.getName).toSeq, Seq("INNERFIRST"))
    }
  }
}

case class Unsafe(c: Char)
case class ParquetTypes(b: Byte, s: Short, ba: Array[Byte])

// It is technically possible to have an optional map, but operation is not bijective
// parquet would read Some(Map.empty) as None
case class MapPrimitive(
  m: Map[String, Int],
  // mo: Option[Map[String, Int]],
  mvo: Map[String, Option[Int]]
)
case class MapNested(
  m: Map[Integers, Nested],
  // mo: Option[Map[Integers, Nested]],
  mvo: Map[Integers, Option[Nested]]
)
case class Decimal(bd: BigDecimal, bdo: Option[BigDecimal])
case class Logical(u: UUID, d: LocalDate)
case class Time(i: Instant, dt: LocalDateTime, ot: OffsetTime, t: LocalTime)
@doc("Parquet with doc")
case class ParquetDoc(@doc("string") s: String, @doc("integers") i: Integers)

@doc("Parquet with doc")
case class ParquetNestedDoc(@doc("nested") pd: ParquetDoc, @doc("integers") i: Integers)

@doc("Parquet with doc with nested list")
case class ParquetNestedListDoc(
  @doc("nested") pd: List[ParquetDoc],
  @doc("integers")
  i: List[Integers]
)

class TestInputFile(ba: Array[Byte]) extends InputFile {
  private val bais = new ByteArrayInputStream(ba)
  override def getLength: Long = ba.length.toLong

  override def newStream(): SeekableInputStream = new DelegatingSeekableInputStream(bais) {
    override def getPos: Long = (ba.length - bais.available()).toLong
    override def seek(newPos: Long): Unit = {
      bais.reset()
      bais.skip(newPos): Unit
    }
  }
}

class TestOutputFile extends OutputFile {
  private val baos = new ByteArrayOutputStream()
  def getBytes: Array[Byte] = baos.toByteArray

  override def create(blockSizeHint: Long): PositionOutputStream = new PositionOutputStream {
    private var pos = 0L
    override def getPos: Long = pos
    override def write(b: Int): Unit = {
      baos.write(b)
      pos += 1
    }
  }

  override def createOrOverwrite(blockSizeHint: Long): PositionOutputStream = ???
  override def supportsBlockSize(): Boolean = false
  override def defaultBlockSize(): Long = 0
}

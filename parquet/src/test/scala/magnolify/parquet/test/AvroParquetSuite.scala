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

package magnolify.parquet.test

import java.time._
import cats._
import magnolify.cats.auto._
import magnolify.avro._
import magnolify.avro.unsafe._
import magnolify.parquet._
import magnolify.parquet.unsafe._
import magnolify.parquet.ParquetArray.AvroCompat._
import magnolify.parquet.test.util.AvroSchemaComparer
import magnolify.scalacheck.auto._
import magnolify.test._
import magnolify.test.Simple._
import magnolify.test.Time._
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.{
  AvroParquetReader,
  AvroParquetWriter,
  AvroReadSupport,
  AvroSchemaConverter,
  GenericDataSupplier
}
import org.scalacheck._

import scala.reflect.ClassTag

class AvroParquetSuite extends MagnolifySuite {
  private def test[T: Arbitrary: ClassTag](implicit
    at: AvroType[T],
    pt: ParquetType[T],
    eq: Eq[T]
  ): Unit = {
    val name = className[T]

    property(s"$name.avro2parquet") {
      Prop.forAll { t: T =>
        val r = at(t)

        val out = new TestOutputFile
        val writer = AvroParquetWriter.builder[GenericRecord](out).withSchema(at.schema).build()
        writer.write(r)
        writer.close()

        val in = new TestInputFile(out.getBytes)
        val reader = pt.readBuilder(in).build()
        val copy = reader.read()
        val next = reader.read()
        reader.close()
        Prop.all(eq.eqv(t, copy), next == null)
      }
    }

    property(s"$name.parquet2avro") {
      Prop.forAll { t: T =>
        val out = new TestOutputFile
        val writer = pt.writeBuilder(out).build()
        writer.write(t)
        writer.close()

        val in = new TestInputFile(out.getBytes)
        val conf = new Configuration()
        AvroReadSupport.setAvroDataSupplier(conf, classOf[GenericDataSupplier])
        val reader = AvroParquetReader.builder[GenericRecord](in).withConf(conf).build()

        val copy = reader.read()
        val next = reader.read()
        reader.close()
        Prop.all(eq.eqv(t, at(copy)), next == null)
      }
    }

    test(s"$name.schema") {
      val converter = new AvroSchemaConverter()
      val avro = converter.convert(at.schema)
      val parquet = pt.schema
      parquet.checkContains(avro)
      avro.checkContains(parquet)
    }

    test(s"$name.avroSchema") {
      val results = avroSchemaComparer.compareEntireSchemas(at.schema, pt.avroSchema)

      assertEquals(results, List())
    }
  }

  test[Integers]
  test[Floats]
  test[Required]
  test[Nullable]

  {
    test[Repeated]
    test[Nested]
  }

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
    implicit val eqByteArray: Eq[Array[Byte]] = Eq.by(_.toList)
    test[ParquetTypes]
  }

  {
    import magnolify.avro.logical.bigquery._
    // Precision = number of digits, so 5 means -99999 to 99999
    val nines = math.pow(10, 38).toLong - 1
    implicit val arbBigDecimal: Arbitrary[BigDecimal] =
      Arbitrary(Gen.choose(-nines, nines).map(BigDecimal(_)))
    implicit val pfBigDecimal: ParquetField[BigDecimal] = ParquetField.decimalBinary(38, 9)
    test[DecimalBinary]
  }

  test[AvroParquetLogical]

  {
    import magnolify.avro.logical.millis._
    import magnolify.parquet.logical.millis._
    test[AvroParquetTimeMillis]
  }

  {
    import magnolify.avro.logical.micros._
    import magnolify.parquet.logical.micros._
    test[AvroParquetTimeMicros]
  }

  test[AvroParquetWithNestedAnnotations]
  test[AvroParquetWithAnnotationsAndOptions]
  test[AvroParquetWithAnnotationsAndLists]

  val avroSchemaComparer = new AvroSchemaComparer {
    override def compareRecordSchemas(s1: Schema, s2: Schema): List[String] = {
      if (s1.getDoc == "Should be ignored" || s1.getDoc == s2.getDoc) {
        List()
      } else {
        List(s"Record ${s1.getType} and ${s2.getType} annotations are not equal")
      }
    }

    override def compareOtherSchemas(s1: Schema, s2: Schema): List[String] = {
      if (
        s1.getType == Schema.Type.ENUM && s2.getType == Schema.Type.STRING ||
        s1.getType == s2.getType
      ) {
        List()
      } else {
        List(s"Schema types are not equal $s1 != $s2")
      }
    }

    override def compareFields(f1: Schema.Field, f2: Schema.Field): List[String] = {
      if (f1.doc() == f2.doc()) {
        List()
      } else {
        List(s"Field ${f1.name()} annotations are not equal")
      }
    }
  }
}

case class AvroParquetLogical(d: LocalDate)
case class AvroParquetTimeMillis(i: Instant, dt: LocalDateTime, t: LocalTime)
case class AvroParquetTimeMicros(i: Instant, dt: LocalDateTime, t: LocalTime)
@doc("Should be ignored")
case class AvroParquetWithAnnotations(
  @doc("nested field policy") s: String,
  s2: String
)
@doc("root level policy")
case class AvroParquetWithNestedAnnotations(
  @doc("string policy") s: String,
  @doc("record policy") nested: AvroParquetWithAnnotations
)

@doc("root level policy")
case class AvroParquetWithAnnotationsAndOptions(
  @doc("string policy") s: String,
  @doc("record policy")
  nested: Option[AvroParquetWithAnnotations]
)

@doc("root level policy")
case class AvroParquetWithAnnotationsAndLists(
  @doc("string policy") s: String,
  @doc("record policy")
  nested: List[AvroParquetWithAnnotations]
)

/*
 * Copyright 2026 Spotify AB
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

import cats.Eq
import magnolify.test.MagnolifySuite
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.{
  AvroDataSupplier,
  AvroParquetReader,
  AvroParquetWriter,
  AvroReadSupport,
  AvroWriteSupport,
  GenericDataSupplier
}
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.metadata.FileMetaData
import org.apache.parquet.io.{LocalInputFile, LocalOutputFile}
import org.apache.parquet.schema.Type
import org.scalacheck.Prop

import java.io.File
import java.nio.file.Files
import scala.jdk.CollectionConverters.*

case class Nested(i: String)
case class TestRecordCompat(a: Int, b: List[String], c: List[Nested], d: Map[String, String])

class ParquetFormatInteropSuite extends MagnolifySuite {
  val AvroSchema = new Schema.Parser().parse(s"""|{
                                                 |  "type":"record",
                                                 |  "name":"TestRecordCompat",
                                                 |  "namespace":"magnolify.parquet",
                                                 |  "fields":[
                                                 |    {"name":"a","type":"int"},
                                                 |    {"name":"b","type":{"type":"array","items":"string"}},
                                                 |    {"name":"c","type":{"type":"array","items":{
                                                 |      "type":"record","name":"array","namespace":"","fields":[{"name":"i","type":"string"}]
                                                 |    }}},
                                                 |    {"name":"d","type":{"type":"map","values":"string"}}]}
                                                 |    """.stripMargin)

  private val nestedSchema = AvroSchema.getField("c").schema().getElementType

  private val typedRecords = (1 to 10).map { i =>
    TestRecordCompat(
      i,
      List(i, i * 2).map(_.toString),
      List(Nested(i.toString)),
      Map("x" -> i.toString)
    )
  }

  private val genericRecords: Seq[GenericRecord] = typedRecords.map { cc =>
    val nested = new GenericData.Record(nestedSchema)
    nested.put("i", cc.c.head.i)
    val record = new GenericData.Record(AvroSchema)
    record.put("a", cc.a)
    record.put("b", cc.b.asJava)
    record.put("c", List(nested).asJava)
    record.put("d", cc.d.asJava)
    record
  }

  private val ptOldListEncoding = ParquetType[TestRecordCompat](
    new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ArrayEncoding.ThreeLevelArray
    }
  )

  private val ptNewListEncoding = ParquetType[TestRecordCompat](
    new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ArrayEncoding.ThreeLevelList
    }
  )

  private def confNewListEncoding(): Configuration = {
    val conf = new Configuration()
    conf.setBoolean(AvroWriteSupport.WRITE_OLD_LIST_STRUCTURE, false)
    conf
  }

  private def confOldListEncoding(): Configuration = {
    val conf = new Configuration()
    conf.setBoolean(AvroWriteSupport.WRITE_OLD_LIST_STRUCTURE, true)
    conf
  }

  def fileMetadata(inputFile: File): FileMetaData = {
    val reader = ParquetFileReader.open(new LocalInputFile(inputFile.toPath))
    val metadata = reader.getFileMetaData
    reader.close()
    metadata
  }

  def testFormatsCompatible(
    pt: ParquetType[TestRecordCompat],
    parquetAvroConf: Configuration
  ): Unit = {
    def tempFile(fileName: String): File = {
      val file = Files.createTempFile(fileName, ".parquet").toFile
      file.delete() // creating a Path creates the file
      file.deleteOnExit()
      file
    }

    val typedOut = tempFile("typed-out")

    // magnolify-parquet write
    {
      val writer = pt
        .writeBuilder(new LocalOutputFile(typedOut.toPath))
        .build()

      typedRecords.foreach(writer.write)
      writer.close()
    }

    // parquet-avro write
    val avroOut = tempFile("avro-out")

    {
      val writer = AvroParquetWriter
        .builder[GenericRecord](new LocalOutputFile(avroOut.toPath))
        .withSchema(AvroSchema)
        .withConf(parquetAvroConf)
        .build()

      genericRecords.foreach(writer.write)
      writer.close()
    }

    // Assert file schemas (and converted avro schemas) match
    val (typedMetadata, avroMetadata) = (fileMetadata(typedOut), fileMetadata(avroOut))
    implicit val eq: Eq[Type] = (x: Type, y: Type) =>
      x.getName == y.getName && x.getRepetition == y.getRepetition && x.isPrimitive == y.isPrimitive

    avroMetadata.getSchema.getFields.asScala
      .zip(typedMetadata.getSchema.getFields.asScala)
      .foreach { case (avroSchema, typedSchema) =>
        Prop.all(eq.eqv(avroSchema, typedSchema))
      }
    assertEquals(
      avroMetadata.getKeyValueMetaData.get("parquet.avro.schema"),
      typedMetadata.getKeyValueMetaData.get("parquet.avro.schema")
    )

    // Validate that written parquet-avro can be read with typed parquet
    {
      val typedReader = pt
        .readBuilder(new LocalInputFile(avroOut.toPath))
        .build()
      assertEquals(typedRecords, (1 to 10).map(_ => typedReader.read()))
      typedReader.close()
    }

    // Validate that written typed-parquet can be read with parquet-avro
    val readConf = new Configuration(parquetAvroConf)
    AvroReadSupport.setRequestedProjection(readConf, AvroSchema)
    AvroReadSupport.setAvroReadSchema(readConf, AvroSchema)
    readConf.setClass(
      AvroReadSupport.AVRO_DATA_SUPPLIER,
      classOf[GenericDataSupplier],
      classOf[AvroDataSupplier]
    )

    {
      val avroReader = AvroParquetReader
        .builder[GenericRecord](new LocalInputFile(typedOut.toPath))
        .withConf(readConf)
        .build()
      assertEquals(genericRecords, (1 to 10).map(_ => avroReader.read()))
      avroReader.close()
    }
  }

  test(
    "magnolify-parquet with new list encoding produces data with equivalent schema as parquet-avro"
  ) {
    testFormatsCompatible(ptNewListEncoding, confNewListEncoding())
  }

  test(
    "magnolify-parquet with old list encoding produces data with equivalent schema as parquet-avro"
  ) {
    testFormatsCompatible(ptOldListEncoding, confOldListEncoding())
  }
}

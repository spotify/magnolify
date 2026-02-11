/*
 * Copyright 2025 Spotify AB
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

import magnolify.parquet.ArrayEncoding.*
import magnolify.test.MagnolifySuite
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.conf.{HadoopParquetConfiguration, ParquetConfiguration}
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.hadoop.api.WriteSupport.WriteContext
import org.apache.parquet.io.{LocalInputFile, LocalOutputFile}
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.{MessageType, MessageTypeParser}

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.*

case class RecordWithListPrimitive(listField: List[Int])

case class Element(i: Int)
case class RecordWithListNested(listField: List[Element])

// Test compatibility with all the valid list encodings from:
// https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists
class ListCompatibilitySuite extends MagnolifySuite {

  val ListField = "listField"
  val recordsWithListPrimitive: Seq[RecordWithListPrimitive] =
    (1 to 10).map(i => RecordWithListPrimitive((0 until i).toList))
  val recordsWithListNested: Seq[RecordWithListNested] =
    (1 to 10).map(i => RecordWithListNested((0 until i).map(Element).toList))

  test("1-level list encoding with primitive list type") {
    val schema = s"""
     |message RecordWith1LevelListPrimitive {
     |  repeated int32 listField (INTEGER(32,true));
     |}
     |""".stripMargin

    roundtripParquet[RecordWithListPrimitive](
      writeSchema = MessageTypeParser.parseMessageType(schema),
      records = recordsWithListPrimitive,
      writeFn = { case (record, rc) =>
        rc.startMessage()

        rc.startField(ListField, 0)
        record.listField.foreach(rc.addInteger)
        rc.endField(ListField, 0)

        rc.endMessage()
      }
    )(typeclass = ParquetType[RecordWithListPrimitive])
  }

  test("2-level list encoding with primitive list type") {
    val schema = s"""
                    |message RecordWith2LevelListNested {
                    |  required group $ListField (LIST) {
                    |    repeated group array {
                    |      required int32 i (INTEGER(32,true));
                    |    }
                    |  }
                    |}
                    |""".stripMargin

    roundtripParquet[RecordWithListNested](
      writeSchema = MessageTypeParser.parseMessageType(schema),
      records = recordsWithListNested,
      writeFn = { case (record, rc) =>
        rc.startMessage()

        rc.startField(ListField, 0)
        rc.startGroup()

        rc.startField("array", 0)
        record.listField.foreach { elem =>
          rc.startGroup()

          rc.startField("i", 0)
          rc.addInteger(elem.i)
          rc.endField("i", 0)

          rc.endGroup()
        }
        rc.endField("array", 0)

        rc.endGroup()
        rc.endField(ListField, 0)

        rc.endMessage()
      }
    )(typeclass = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelArray
    }))
  }

  test("3-Level list encoding with primitive list type") {
    val typeClass = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
    })

    val schema =
      s"""
         |message magnolify.parquet.RecordWithListPrimitive {
         |  required group listField (LIST) {
         |    repeated group list {
         |      required int32 element (INTEGER(32,true));
         |    }
         |  }
         |}
         |""".stripMargin

    assertEquals(typeClass.schema, MessageTypeParser.parseMessageType(schema))

    roundtripParquet[RecordWithListPrimitive](
      writeSchema = MessageTypeParser.parseMessageType(schema),
      records = recordsWithListPrimitive,
      writeFn = { case (record, rc) =>
        rc.startMessage()

        rc.startField(ListField, 0)
        rc.startGroup()

        /** Reference: [[org.apache.parquet.avro.AvroWriteSupport#ThreeLevelListWriter]] */
        rc.startField("list", 0)
        record.listField.foreach { elem =>
          rc.startGroup()

          rc.startField("element", 0)
          rc.addInteger(elem)
          rc.endField("element", 0)

          rc.endGroup()
        }

        rc.endField("list", 0)
        rc.endGroup()
        rc.endField(ListField, 0)

        rc.endMessage()
      }
    )(typeClass)
  }

  test("3-Level list encoding with nested list type") {
    val typeClass = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
    })

    val schema =
      s"""
         |message magnolify.parquet.RecordWithListNested {
         |  required group listField (LIST) {
         |    repeated group list {
         |      required group element {
         |        required int32 i (INTEGER(32,true));
         |      }
         |    }
         |  }
         |}
         |""".stripMargin

    assertEquals(typeClass.schema, MessageTypeParser.parseMessageType(schema))

    roundtripParquet[RecordWithListNested](
      writeSchema = MessageTypeParser.parseMessageType(schema),
      records = recordsWithListNested,
      writeFn = { case (record, rc) =>
        rc.startMessage()

        rc.startField(ListField, 0)
        rc.startGroup()

        rc.startField("list", 0)
        record.listField.foreach { elem =>
          rc.startGroup()

          rc.startField("element", 0)
          rc.startGroup()

          rc.startField("i", 0)
          rc.addInteger(elem.i)
          rc.endField("i", 0)

          rc.endGroup()
          rc.endField("element", 0)

          rc.endGroup()
        }

        rc.endField("list", 0)
        rc.endGroup()
        rc.endField(ListField, 0)

        rc.endMessage()
      }
    )(typeClass)
  }

  class LocalWriteBuilder[T](file: LocalOutputFile, writeSupport: WriteSupport[T])
      extends ParquetWriter.Builder[T, LocalWriteBuilder[T]](file) {
    override def self(): LocalWriteBuilder[T] = this
    override def getWriteSupport(conf: Configuration): WriteSupport[T] = writeSupport
  }

  private def roundtripParquet[T](
    writeSchema: MessageType,
    records: Seq[T],
    writeFn: (T, RecordConsumer) => Unit
  )(typeclass: ParquetType[T]): Unit = {
    // Write files manually using provided writer fn
    val manuallyWrittenRecords =
      Files.createTempFile(s"parquet-list-compat-manual-${writeSchema.getName}", ".parquet").toFile

    // Write to temp file using provided writer fn
    {
      manuallyWrittenRecords.delete() // creating a Path creates the file
      manuallyWrittenRecords.deleteOnExit()

      val writePath = new LocalOutputFile(Paths.get(manuallyWrittenRecords.toString))
      val writer: ParquetWriter[T] = new LocalWriteBuilder[T](
        writePath,
        new WriteSupport[T]() {
          var rc: RecordConsumer = null

          override def init(configuration: Configuration): WriteContext =
            init(new HadoopParquetConfiguration(configuration))

          override def init(configuration: ParquetConfiguration): WriteContext =
            new WriteContext(writeSchema, Map[String, String]().asJava)

          override def prepareForWrite(recordConsumer: RecordConsumer): Unit =
            this.rc = recordConsumer

          override def write(record: T): Unit = writeFn(record, rc)
        }
      ).build()

      records.foreach(writer.write)
      writer.close()
    }

    // Check that manually written records match Magnolify writer functionality
    val magnolifyWrittenRecords =
      Files
        .createTempFile(s"parquet-list-compat-magnolify-${writeSchema.getName}", ".parquet")
        .toFile

    {
      magnolifyWrittenRecords.delete() // creating a Path creates the file
      magnolifyWrittenRecords.deleteOnExit()

      val writer = typeclass
        .writeBuilder(new LocalOutputFile(magnolifyWrittenRecords.toPath))
        .build()

      records.foreach(writer.write)
      writer.close()
    }

    // Read using typeclass
    val readManuallyWrittenRecords = {
      val reader = typeclass
        .readBuilder(new LocalInputFile(manuallyWrittenRecords.toPath))
        .build()

      val records = (1 to 10).map(_ => reader.read())
      reader.close()
      records
    }

    val readMagnolifyWrittenRecords = {
      val reader = typeclass
        .readBuilder(new LocalInputFile(magnolifyWrittenRecords.toPath))
        .build()

      val records = (1 to 10).map(_ => reader.read())
      reader.close()
      records
    }

    // Assert that records match expected
    assertEquals(readManuallyWrittenRecords, records)

    // Assert that magnolify writer matches expected record format
    assertEquals(readMagnolifyWrittenRecords, readManuallyWrittenRecords)
  }
}

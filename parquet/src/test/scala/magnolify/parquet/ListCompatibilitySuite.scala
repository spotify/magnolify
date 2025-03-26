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

import magnolify.test.MagnolifySuite
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.conf.{HadoopParquetConfiguration, ParquetConfiguration}
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.hadoop.api.WriteSupport.WriteContext
import org.apache.parquet.io.LocalInputFile
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.{MessageType, MessageTypeParser}

import java.nio.file.Files
import scala.annotation.nowarn
import scala.jdk.CollectionConverters.*

case class RecordWithListPrimitive(listField: List[Int])

case class Element(i: Int)
case class RecordWithListNested(listField: List[Element])

// Test compatibility with all the valid list encodings from:
// https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists
@nowarn("cat=deprecation")
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
    )(readTypeclass = ParquetType[RecordWithListPrimitive])
  }

  test("2-level list encoding with primitive list type when AvroCompat mode is enabled on read") {
    val schema = s"""
      |message RecordWith2LevelListPrimitive {
      |  required group $ListField (LIST) {
      |    repeated int32 array (INTEGER(32,true));
      |  }
      |}
      |""".stripMargin

    roundtripParquet[RecordWithListPrimitive](
      writeSchema = MessageTypeParser.parseMessageType(schema),
      records = recordsWithListPrimitive,
      writeFn = { case (record, rc) =>
        rc.startMessage()

        rc.startField(ListField, 0)
        rc.startGroup()

        rc.startField("array", 0)
        record.listField.foreach(rc.addInteger)
        rc.endField("array", 0)

        rc.endGroup()
        rc.endField(ListField, 0)

        rc.endMessage()
      }
    )(readTypeclass = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def WriteAvroCompatibleArrays: Boolean = true
    }))
  }

  test("2-level list encoding with nested list type when AvroCompat mode is enabled on read") {
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
    )(readTypeclass = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
      override def WriteAvroCompatibleArrays: Boolean = true
    }))
  }

  // Fails, lists are deserialized as Nil
  test("3-Level list encoding with primitive list type when AvroCompat mode is enabled on read") {
    val schema = s"""
      |message RecordWith3LevelListPrimitive {
      |  required group listField (LIST) {
      |    repeated group list {
      |      required int32 element;
      |    }
      |  }
      |}
      |""".stripMargin
    roundtripParquet[RecordWithListPrimitive](
      writeSchema = MessageTypeParser.parseMessageType(schema),
      records = recordsWithListPrimitive,
      writeFn = { case (record, rc) =>
        rc.startMessage()

        rc.startField(ListField, 0)
        rc.startGroup()

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
    )(readTypeclass = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def WriteAvroCompatibleArrays: Boolean = true
    }))
  }

  private def roundtripParquet[T](
    writeSchema: MessageType,
    records: Seq[T],
    writeFn: (T, RecordConsumer) => Unit
  )(readTypeclass: ParquetType[T]): Unit = {
    val tempFile =
      Files.createTempFile(s"parquet-list-compat-${writeSchema.getName}", ".parquet").toFile
    val path = new Path(tempFile.toString)
    tempFile.delete() // creating a Path creates the file
    tempFile.deleteOnExit()

    // Write to temp file
    val writer: ParquetWriter[T] = new ParquetWriter(
      path,
      new WriteSupport[T]() {
        var rc: RecordConsumer = null

        override def init(configuration: Configuration): WriteSupport.WriteContext =
          init(new HadoopParquetConfiguration(configuration))

        override def init(configuration: ParquetConfiguration): WriteSupport.WriteContext =
          new WriteContext(writeSchema, Map[String, String]().asJava)

        override def prepareForWrite(recordConsumer: RecordConsumer): Unit =
          this.rc = recordConsumer

        override def write(record: T): Unit = writeFn(record, rc)
      }
    )
    records.foreach(writer.write)
    writer.close()

    // Read using typeclass
    val reader = readTypeclass
      .readBuilder(new LocalInputFile(tempFile.toPath))
      .build()

    val readRecords = (1 to 10).map(_ => reader.read())
    reader.close()

    assertEquals(readRecords, records)
  }
}

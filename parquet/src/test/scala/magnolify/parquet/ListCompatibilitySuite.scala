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
import org.apache.parquet.hadoop.api.{InitContext, WriteSupport}
import org.apache.parquet.hadoop.api.WriteSupport.WriteContext
import org.apache.parquet.io.{InvalidRecordException, LocalInputFile, LocalOutputFile}
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.{MessageType, MessageTypeParser}

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

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
    implicit val typeclass = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = Ungrouped
    })

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
    )
  }

  test("2-level list encoding with primitive list type") {
    implicit val typeclass = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelArray
    })

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
    )
  }

  test("3-Level list encoding with primitive list type") {
    implicit val typeClass = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
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
    )
  }

  test("3-Level writer schema fails with 2-level reader when auto-detection is disabled") {
    val conf = new Configuration()
    conf.setBoolean(MagnolifyParquetProperties.AutoDetectListEncoding, false)
    val typeClassList = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
    })

    val typeClassArray = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelArray
    })

    Try {
      roundtripParquet[RecordWithListPrimitive](
        writeSchema = typeClassList.schema,
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
        },
        readerConf = conf
      )(typeClassList, typeClassArray)
    } match {
      case Failure(_: InvalidRecordException) => // success
      case Failure(e)                         =>
        fail(
          s"Expected mismatched list encoding types to throw InvalidRecordException, but caught error $e"
        )
      case Success(_) =>
        fail(s"Expected mismatched list encoding types to throw InvalidRecordException")
    }
  }

  test("2-Level writer schema should fail to be read by 3-level reader schema") {
    val typeClassList = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
    })

    val typeClassArray = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelArray
    })

    Try {
      roundtripParquet[RecordWithListNested](
        writeSchema = typeClassArray.schema,
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
      )(typeClassArray, typeClassList)
    } match {
      case Failure(_: InvalidRecordException) => // success
      case Failure(e)                         =>
        fail(
          s"Expected mismatched list encoding types to throw InvalidRecordException, but caught error $e"
        )
      case Success(_) =>
        fail(s"Expected mismatched list encoding types to throw InvalidRecordException")
    }
  }

  private val autoDetectConf = {
    val conf = new Configuration()
    // It's true by default, but be explicit for test
    conf.setBoolean(MagnolifyParquetProperties.AutoDetectListEncoding, true)
    conf
  }

  test("3-Level writer schema auto-detected by 2-level reader schema") {
    val typeClassList = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
    })

    val typeClassArray = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelArray
    })

    roundtripParquet[RecordWithListPrimitive](
      writeSchema = typeClassList.schema,
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
      },
      readerConf = autoDetectConf
    )(typeClassList, typeClassArray)
  }

  test("auto-detection updates the reader without mutating its typeclass or configuration") {
    val readerType = ParquetType[RecordWithListPrimitive]
    val writerType = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
    })
    val conf = new Configuration(autoDetectConf)
    val originalConf = conf.iterator().asScala.map(e => e.getKey -> e.getValue).toMap
    val context = new InitContext(
      conf,
      Map.empty[String, java.util.Set[String]].asJava,
      writerType.schema
    )

    assertEquals(readerType.readSupport.init(context).getRequestedSchema, writerType.schema)
    assertEquals(readerType.properties.writeArrayEncoding, Ungrouped)
    assertEquals(conf.iterator().asScala.map(e => e.getKey -> e.getValue).toMap, originalConf)
  }

  test("auto-detection defaults to enabled and uses the Magnolify config key") {
    val readerType = ParquetType[RecordWithListPrimitive]
    val writerType = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
    })
    for {
      enabled <- Seq(None, Some(true), Some(false))
      avroEnabled <- Seq(true, false)
    } {
      val conf = new Configuration(false)
      conf.setBoolean("parquet.avro.read.autoDetectListStructure", avroEnabled)
      enabled.foreach(conf.setBoolean(MagnolifyParquetProperties.AutoDetectListEncoding, _))
      val context = new InitContext(
        conf,
        Map.empty[String, java.util.Set[String]].asJava,
        writerType.schema
      )
      if (enabled.contains(false)) {
        intercept[InvalidRecordException] {
          readerType.readSupport.init(context)
        }
      } else {
        assertEquals(
          readerType.readSupport.init(context).getRequestedSchema,
          writerType.schema,
          clues(enabled, avroEnabled)
        )
      }
    }
  }

  test("2-Level writer schema is not auto-detected by 3-level reader schema") {
    val typeClassList = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
    })
    val typeClassArray = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
      override def writeArrayEncoding: ArrayEncoding = ThreeLevelArray
    })

    val context = new InitContext(
      autoDetectConf,
      Map.empty[String, java.util.Set[String]].asJava,
      typeClassArray.schema
    )
    intercept[InvalidRecordException] {
      typeClassList.readSupport.init(context)
    }
  }

  test("3-Level list encoding with nested list type") {
    implicit val typeClass = ParquetType[RecordWithListNested](new MagnolifyParquetProperties {
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
    )
  }

  class LocalWriteBuilder[T](file: LocalOutputFile, writeSupport: WriteSupport[T])
      extends ParquetWriter.Builder[T, LocalWriteBuilder[T]](file) {
    override def self(): LocalWriteBuilder[T] = this
    override def getWriteSupport(conf: Configuration): WriteSupport[T] = writeSupport
  }

  private def roundtripParquet[T](
    writeSchema: MessageType,
    records: Seq[T],
    writeFn: (T, RecordConsumer) => Unit,
    readerConf: Configuration = new Configuration()
  )(implicit writerTypeclass: ParquetType[T], readerTypeclass: ParquetType[T]): Unit = {
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

      val writer = writerTypeclass
        .writeBuilder(new LocalOutputFile(magnolifyWrittenRecords.toPath))
        .build()

      records.foreach(writer.write)
      writer.close()
    }

    // Read using typeclass
    val readManuallyWrittenRecords = {
      val reader = readerTypeclass
        .readBuilder(new LocalInputFile(manuallyWrittenRecords.toPath))
        .withConf(readerConf)
        .build()

      val records = (1 to 10).map(_ => reader.read())
      reader.close()
      records
    }

    val readMagnolifyWrittenRecords = {
      val reader = readerTypeclass
        .readBuilder(new LocalInputFile(magnolifyWrittenRecords.toPath))
        .withConf(readerConf)
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

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

import magnolify.parquet.ArrayEncoding._
import magnolify.test.MagnolifySuite
import org.apache.parquet.io.InvalidRecordException
import org.apache.parquet.schema.MessageTypeParser

object SchemaSuite {
  case class RecordWithPrimitiveList(l: List[Int])

  case class Nested(i: Int)
  case class RecordWithNestedList(n: Nested)

  val PtUngroupedList = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
    override def writeArrayEncoding: ArrayEncoding = Ungrouped
  })

  val PtThreeLevelListEnc = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
    override def writeArrayEncoding: ArrayEncoding = ThreeLevelList
  })

  val PtThreeLevelArrayEnc = ParquetType[RecordWithListPrimitive](new MagnolifyParquetProperties {
    override def writeArrayEncoding: ArrayEncoding = ThreeLevelArray
  })
}

class SchemaSuite extends MagnolifySuite {
  import SchemaSuite._

  private val schemaNoListFields = MessageTypeParser.parseMessageType(
    """message Record {
      |  required int32 i1;
      |  required group nestedGroup {
      |    required int32 i2;
      |  }
      |}""".stripMargin
  )

  test("checkCompatibility: 3-level list writer compatible with 3-level list reader") {
    Schema.checkCompatibility(PtThreeLevelListEnc.schema, PtThreeLevelListEnc.schema)
  }

  test("checkCompatibility: 3-level array writer compatible with 3-level array reader") {
    Schema.checkCompatibility(PtThreeLevelArrayEnc.schema, PtThreeLevelArrayEnc.schema)
  }

  test("checkCompatibility: 3-level list writer incompatible with 3-level array reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(PtThreeLevelListEnc.schema, PtThreeLevelArrayEnc.schema)
    }
  }

  test("checkCompatibility: 3-level list writer incompatible with ungrouped reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(PtThreeLevelListEnc.schema, PtUngroupedList.schema)
    }
  }

  test("checkCompatibility: 3-level array writer incompatible with 3-level list reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(PtThreeLevelArrayEnc.schema, PtThreeLevelListEnc.schema)
    }
  }

  test("checkCompatibility: 3-level array writer incompatible with 3-level list reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(PtThreeLevelArrayEnc.schema, PtUngroupedList.schema)
    }
  }

  test("checkCompatibility: ungrouped writer incompatible with 3-level list reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(PtUngroupedList.schema, PtThreeLevelListEnc.schema)
    }
  }

  test("checkCompatibility: ungrouped writer incompatible with 3-level array reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(PtUngroupedList.schema, PtThreeLevelArrayEnc.schema)
    }
  }

  test("checkCompatibility: primitive schemas are compatible") {
    Schema.checkCompatibility(PtUngroupedList.schema, PtUngroupedList.schema)
  }

  test("checkCompatibility: reader with 3 level list field not in writer is compatible") {
    Schema.checkCompatibility(schemaNoListFields, PtThreeLevelListEnc.schema)
  }

  test("checkCompatibility: reader with 3 level array field not in writer is compatible") {
    Schema.checkCompatibility(schemaNoListFields, PtThreeLevelArrayEnc.schema)
  }

  test("checkCompatibility: reader with ungrouped list field not in writer is compatible") {
    Schema.checkCompatibility(schemaNoListFields, PtUngroupedList.schema)
  }

  test("checkCompatibility: reader with map field not in writer is compatible") {
    Schema.checkCompatibility(
      schemaNoListFields,
      MessageTypeParser.parseMessageType(
        """message Record {
        |  required group my_map (MAP) {
        |    repeated group key_value {
        |      required binary key (STRING);
        |      optional int32 value;
        |    }
        |  }
        |}""".stripMargin
      )
    )
  }

  test("checkCompatibility: reader with optional field not in writer is compatible") {
    val writer = MessageTypeParser.parseMessageType(
      """message Record {
        |  required int32 a (INTEGER(32,true));
        |}""".stripMargin
    )
    val reader = MessageTypeParser.parseMessageType(
      """message Record {
        |  required int32 a (INTEGER(32,true));
        |  optional int32 b (INTEGER(32,true));
        |}""".stripMargin
    )
    Schema.checkCompatibility(writer, reader)
  }

  test("checkCompatibility: reader with required field not in writer fails") {
    val writer = MessageTypeParser.parseMessageType(
      """message Record {
        |  required int32 a (INTEGER(32,true));
        |}""".stripMargin
    )
    val reader = MessageTypeParser.parseMessageType(
      """message Record {
        |  required int32 a (INTEGER(32,true));
        |  required int32 b (INTEGER(32,true));
        |}""".stripMargin
    )
    val e = intercept[InvalidRecordException] {
      Schema.checkCompatibility(writer, reader)
    }
    assert(e.getMessage.contains("not present"))
  }
}

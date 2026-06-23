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

import magnolify.test.MagnolifySuite
import org.apache.parquet.io.InvalidRecordException
import org.apache.parquet.schema.MessageTypeParser

class SchemaSuite extends MagnolifySuite {

  private val schemaNoListFields = MessageTypeParser.parseMessageType(
    """message Record {
      |  required int32 i1;
      |  required group nestedGroup {
      |    required int32 i2;
      |  }
      |}""".stripMargin
  )

  private val threeLevelListSchema = MessageTypeParser.parseMessageType(
    """message Record {
      |  required group nestedGroup {
      |    required group listField (LIST) {
      |      repeated group list {
      |        required int32 element (INTEGER(32,true));
      |      }
      |    }
      |  }
      |}""".stripMargin
  )

  private val threeLevelArraySchema = MessageTypeParser.parseMessageType(
    """message Record {
      |  required group nestedGroup {
      |    required group listField (LIST) {
      |      repeated group array {
      |        required int32 element (INTEGER(32,true));
      |      }
      |    }
      |  }
      |}""".stripMargin
  )

  private val ungroupedSchema = MessageTypeParser.parseMessageType(
    """message Record {
      |  required group nestedGroup {
      |    repeated int32 listField (INTEGER(32,true));
      |  }
      |}""".stripMargin
  )

  private val primitiveSchema = MessageTypeParser.parseMessageType(
    """message Record {
      |  required int32 listField (INTEGER(32,true));
      |}""".stripMargin
  )

  private val mapSchema = MessageTypeParser.parseMessageType(
    """message Record {
      |  required group my_map (MAP) {
      |    repeated group key_value {
      |      required binary key (STRING);
      |      optional int32 value;
      |    }
      |  }
      |}""".stripMargin
  )

  test("checkCompatibility: 3-level list writer compatible with 3-level list reader") {
    Schema.checkCompatibility(threeLevelListSchema, threeLevelListSchema)
  }

  test("checkCompatibility: 3-level array writer compatible with 3-level array reader") {
    Schema.checkCompatibility(threeLevelArraySchema, threeLevelArraySchema)
  }

  test("checkCompatibility: 3-level list writer incompatible with 3-level array reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(threeLevelListSchema, threeLevelArraySchema)
    }
  }

  test("checkCompatibility: 3-level array writer incompatible with 3-level list reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(threeLevelArraySchema, threeLevelListSchema)
    }
  }

  test("checkCompatibility: ungrouped writer incompatible with 3-level list reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(ungroupedSchema, threeLevelListSchema)
    }
  }

  test("checkCompatibility: ungrouped writer incompatible with 3-level array reader") {
    intercept[InvalidRecordException] {
      Schema.checkCompatibility(ungroupedSchema, threeLevelArraySchema)
    }
  }

  test("checkCompatibility: primitive schemas are compatible") {
    Schema.checkCompatibility(primitiveSchema, primitiveSchema)
  }

  test("checkCompatibility: reader with 3 level list field not in writer is not compatible") {
    val e = intercept[InvalidRecordException] {
      Schema.checkCompatibility(schemaNoListFields, threeLevelListSchema)
    }
    assert(e.getMessage.contains("is not present in written file schema"))
  }

  test("checkCompatibility: reader with 3 level array field not in writer is not compatible") {
    val e = intercept[InvalidRecordException] {
      Schema.checkCompatibility(schemaNoListFields, threeLevelArraySchema)
    }
    assert(e.getMessage.contains("is not present in written file schema"))
  }

  test("checkCompatibility: reader with map field not in writer is not compatible") {
    val e = intercept[InvalidRecordException] {
      Schema.checkCompatibility(schemaNoListFields, mapSchema)
    }
    assert(e.getMessage.contains("is not present in written file schema"))
  }

  test("checkCompatibility: reader with ungrouped list field not in writer is not compatible") {
    val e = intercept[InvalidRecordException] {
      Schema.checkCompatibility(schemaNoListFields, ungroupedSchema)
    }
    assert(e.getMessage.contains("is not present in written file schema"))
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

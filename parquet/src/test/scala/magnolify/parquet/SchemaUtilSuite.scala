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

import magnolify.parquet.util.AvroSchemaComparer
import magnolify.test.MagnolifySuite
import org.apache.avro.{Schema => AvroSchema}

class SchemaUtilSuite extends MagnolifySuite {

  test(s"schema") {
    val nestedSchema =
      """
        |{
        |  "type" : "record",
        |  "name" : "Integers",
        |  "namespace" : "magnolify.test.Simple",
        |  "fields" : [ {
        |    "name" : "i",
        |    "type" : "int"
        |  }, {
        |    "name" : "l",
        |    "type" : "long"
        |  } ]
        |}
        |""".stripMargin
    val inputSchema = new AvroSchema.Parser().parse(nestedSchema)

    val outputSchema = SchemaUtil.deepCopy(
      inputSchema,
      Some("root level"),
      _ => Some("field level")
    )

    val results = AvroSchemaComparer.compareSchemas(inputSchema, outputSchema)
    assertEquals(
      results,
      List(
        "root 'doc' are different 'null' != 'root level'",
        "root.i field 'doc' are different 'null' != 'field level'",
        "root.l field 'doc' are different 'null' != 'field level'"
      )
    )
  }
}

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

import magnolify.parquet.SchemaUtil
import magnolify.parquet.test.util.AvroSchemaComparer
import magnolify.test.MagnolifySuite
import org.apache.avro.Schema

class SchemaUtilSuite extends MagnolifySuite {

  test(s"schema") {
    val comparer = new AvroSchemaComparer {
      override def compareRecordSchemas(s1: Schema, s2: Schema): List[String] =
        if (s2.getDoc == "root level") List() else List("type doc not set")

      override def compareOtherSchemas(s1: Schema, s2: Schema): List[String] = if (s1.equals(s2))
        List()
      else List(s"$s1 != $s2")

      override def compareFields(s1: Schema.Field, s2: Schema.Field): List[String] =
        if (s2.doc() == "field level") List() else List("field doc not set")
    }

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
    val parser = new Schema.Parser()
    val inputSchema = parser.parse(nestedSchema)

    val outputSchema = SchemaUtil.deepCopy(
      inputSchema,
      Some("root level"),
      _ => Some("field level")
    )

    val results = comparer.compareEntireSchemas(inputSchema, outputSchema)
    assertEquals(results, List())
  }
}

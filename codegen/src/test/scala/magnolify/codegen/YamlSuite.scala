/*
 * Copyright 2020 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.codegen

import magnolify.codegen.Yaml._

class YamlSuite extends munit.FunSuite {
  test("empty") {
    assertEquals(parse(""), Schema())
  }

  test("avro") {
    val schema = parse("""
        |avro:
        |  avsc:
        |    - a.avsc
        |    - b.avsc
        |  avro:
        |    - a.avro
        |    - b.avro
        |  class:
        |    - magnolify.codegen.avro.RecordA
        |    - magnolify.codegen.avro.RecordB
        |""".stripMargin)
    val expected = Schema(avro =
      Some(
        Avro(
          List("a.avsc", "b.avsc"),
          List("a.avro", "b.avro"),
          List("magnolify.codegen.avro.RecordA", "magnolify.codegen.avro.RecordB")
        )
      )
    )
    assertEquals(schema, expected)
  }

  test("avro params") {
    val params = parse("""
        |avro:
        |  params:
        |    case_mapper: snake_to_camel
        |    fallback_namespace: unknown
        |    relocate_namespace:
        |      prefix: shaded.
        |      suffix: .avro
        |    array_type: mutable.Buffer
        |    map_type: mutable.Map
        |    type_overrides:
        |      BYTES: ByteString
        |    extra_imports:
        |      - com.google.protobuf.ByteString
        |""".stripMargin).avro.get.params.get
    assertEquals(params.caseMapper.map("foo_bar"), "fooBar")
    assertEquals(params.fallbackNamespace, Some("unknown"))
    assertEquals(params.relocateNamespace("magnolify.codegen"), "shaded.magnolify.codegen.avro")
    assertEquals(params.arrayType, "mutable.Buffer")
    assertEquals(params.mapType, "mutable.Map")
    assertEquals(params.typeOverrides, Map(org.apache.avro.Schema.Type.BYTES -> "ByteString"))
    assertEquals(params.extraImports, List("com.google.protobuf.ByteString"))
  }

  test("bigquery") {
    val schema = parse("""
        |bigquery:
        |  from_schema:
        |    - json: schema1.json
        |      name: Record1
        |      namespace: magnolify.codegen.bigquery
        |      description: table description
        |    - json: schema2.json
        |      name: Record2
        |      namespace: magnolify.codegen.bigquery
        |  from_table:
        |    - table: project1:dataset1.table1
        |      name: Record3
        |      namespace: magnolify.codegen.bigquery
        |      description: table description
        |    - table: project2:dataset2.table2
        |      name: Record4
        |      namespace: magnolify.codegen.bigquery
        |  from_query:
        |    - query: SELECT a, b FROM table1
        |      name: Record5
        |      namespace: magnolify.codegen.bigquery
        |      description: table description
        |    - query: SELECT c, d FROM table2
        |      name: Record6
        |      namespace: magnolify.codegen.bigquery
        |  from_storage:
        |    - table: project1:dataset1.table1
        |      name: Record7
        |      namespace: magnolify.codegen.bigquery
        |      description: table description
        |      selected_fields:
        |        - a
        |        - b
        |      row_restriction: a > 0
        |    - table: project2:dataset2.table2
        |      name: Record8
        |      namespace: magnolify.codegen.bigquery
        |""".stripMargin)
    val ns = "magnolify.codegen.bigquery"
    val desc = Some("table description")
    val expected = Schema(bigquery =
      Some(
        BigQuery(
          fromSchema = List(
            BigQuery.FromSchema("schema1.json", "Record1", ns, desc),
            BigQuery.FromSchema("schema2.json", "Record2", ns)
          ),
          fromTable = List(
            BigQuery.FromTable("project1:dataset1.table1", "Record3", ns, desc),
            BigQuery.FromTable("project2:dataset2.table2", "Record4", ns)
          ),
          fromQuery = List(
            BigQuery.FromQuery("SELECT a, b FROM table1", "Record5", ns, desc),
            BigQuery.FromQuery("SELECT c, d FROM table2", "Record6", ns)
          ),
          fromStorage = List(
            BigQuery.FromStorage(
              "project1:dataset1.table1",
              "Record7",
              ns,
              desc,
              List("a", "b"),
              Some("a > 0")
            ),
            BigQuery.FromStorage("project2:dataset2.table2", "Record8", ns)
          )
        )
      )
    )
    assertEquals(schema, expected)
  }

  test("bigquery params") {
    val params = parse("""
        |bigquery:
        |  params:
        |    case_mapper: snake_to_camel
        |    repeated_type: mutable.Buffer
        |    type_overrides:
        |      BYTES: ByteString
        |    extra_imports:
        |      - com.google.protobuf.ByteString
        |""".stripMargin).bigquery.get.params.get
    assertEquals(params.caseMapper.map("foo_bar"), "fooBar")
    assertEquals(params.repeatedType, "mutable.Buffer")
    assertEquals(params.typeOverrides, Map("BYTES" -> "ByteString"))
    assertEquals(params.extraImports, List("com.google.protobuf.ByteString"))
  }
  test("protobuf") {
    val schema = parse("""
        |protobuf:
        |  proto:
        |    - a.pb
        |    - b.pb
        |  class:
        |    - magnolify.codegen.Proto.RecordA
        |    - magnolify.codegen.Proto.RecordB
        |""".stripMargin)
    val expected = Schema(protobuf =
      Some(
        Protobuf(
          List("a.pb", "b.pb"),
          List("magnolify.codegen.Proto.RecordA", "magnolify.codegen.Proto.RecordB")
        )
      )
    )
  }

  test("protobuf params") {
    val params = parse("""
        |protobuf:
        |  params:
        |    case_mapper: snake_to_camel
        |    fallback_namespace: unknown
        |    relocate_namespace:
        |      prefix: shaded.
        |      suffix: .proto
        |    proto3_option: true
        |    repeated_type: mutable.Buffer
        |    type_overrides:
        |      STRING: URI
        |    extra_imports:
        |      - java.net.URI
        |""".stripMargin).protobuf.get.params
    assertEquals(params.caseMapper.get.map("foo_bar"), "fooBar")
    assertEquals(params.fallbackNamespace, Some("unknown"))
    assertEquals(
      params.relocateNamespace.get("magnolify.codegen"),
      "shaded.magnolify.codegen.proto"
    )
    assert(params.proto3Option)
    assertEquals(params.repeatedType, "mutable.Buffer")
    assertEquals(
      params.typeOverrides,
      Map(com.google.protobuf.Descriptors.FieldDescriptor.Type.STRING -> "URI")
    )
    assertEquals(params.extraImports, List("java.net.URI"))
  }
}

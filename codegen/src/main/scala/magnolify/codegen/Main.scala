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

import java.io.File

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: codegen YAML DST")
      sys.exit(1)
    }
    val yaml = Yaml.parse(new File(args(0)))
    val dst = new File(args(1))
    dst.mkdirs()
    yaml.avro.foreach(genAvro(_, dst))
    yaml.bigquery.foreach(genBigQuery(_, dst))
    yaml.protobuf.foreach(genProtobuf(_, dst))
  }

  private def genAvro(yaml: Yaml.Avro, dst: File): Unit = {
    val avscs = yaml.avsc.map(AvroClient.fromAvsc)
    val avros = yaml.avro.map(AvroClient.fromAvro)
    val classes = yaml.`class`.map(AvroClient.fromClass)

    (avscs ++ avros ++ classes)
      .flatMap(AvroGen.gen(_, yaml.params.get))
      .foreach(_.saveTo(dst.toPath))
  }

  private def genBigQuery(yaml: Yaml.BigQuery, dst: File): Unit = {
    val schemas = yaml.fromSchema.map { f =>
      val schema = BigQueryClient.fromSchema(f.json)
      TableRowGen.gen(schema, f.name, f.namespace, f.description, yaml.params.get)
    }
    val tables = yaml.fromTable.map { f =>
      val schema = BigQueryClient.fromTable(f.table)
      TableRowGen.gen(schema, f.name, f.namespace, f.description, yaml.params.get)
    }
    val queries = yaml.fromQuery.map { f =>
      val schema = BigQueryClient.fromQuery(f.query)
      TableRowGen.gen(schema, f.name, f.namespace, f.description, yaml.params.get)
    }
    val storages = yaml.fromStorage.map { f =>
      val schema = BigQueryClient.fromStorage(f.table, f.selectedFields, f.rowRestriction)
      TableRowGen.gen(schema, f.name, f.namespace, f.description, yaml.params.get)
    }

    (schemas ++ tables ++ queries ++ storages)
      .foreach(_.saveTo(dst.toPath))
  }

  private def genProtobuf(yaml: Yaml.Protobuf, dst: File): Unit = {
    val protos = yaml.proto.flatMap(ProtobufClient.fromProto)
    val classes = yaml.`class`.map(ProtobufClient.fromClass)
    (protos ++ classes)
      .flatMap(ProtobufGen.gen(_, yaml.params.get))
      .foreach(_.saveTo(dst.toPath))
  }
}

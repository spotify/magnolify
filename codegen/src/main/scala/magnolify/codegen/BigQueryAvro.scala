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

import com.google.api.services.bigquery.model.{TableFieldSchema, TableSchema}
import magnolify.shims.JavaConverters._
import org.apache.avro.Schema

object BigQueryAvro {
  def fromAvro(schema: Schema): TableSchema = new TableSchema().setFields(toFields(schema).asJava)

  // FIXME: support logical types
  private val typeMap: Map[Schema.Type, String] = Map(
    Schema.Type.STRING -> "STRING",
    Schema.Type.BYTES -> "BYTES",
    Schema.Type.LONG -> "INT64",
    Schema.Type.DOUBLE -> "FLOAT64",
    Schema.Type.BOOLEAN -> "BOOLEAN"
  )

  private def toFields(schema: Schema): List[TableFieldSchema] =
    schema.getFields.asScala.map { f =>
      val field = parseField(f.schema())
      new TableFieldSchema()
        .setName(f.name())
        .setType(field.tpe)
        .setMode(field.mode)
        .setFields(field.fields.asJava)
        .setDescription(f.doc())
    }.toList

  private def parseField(schema: Schema): Field = {
    if (schema.getType == Schema.Type.RECORD) {
      Field("STRUCT", "REQUIRED", toFields(schema))
    } else if (schema.getType == Schema.Type.ARRAY) {
      parseField(schema.getElementType).copy(mode = "REPEATED")
    } else if (isNullable(schema)) {
      parseField(schema.getTypes.get(1)).copy(mode = "NULLABLE")
    } else {
      typeMap.get(schema.getType) match {
        case Some(tpe) => Field(tpe, "REQUIRED", Nil)
        case None      => throw new IllegalArgumentException(s"Unsupported type: ${schema.getType}")
      }
    }
  }

  private def isNullable(schema: Schema): Boolean =
    schema.getType == Schema.Type.UNION &&
      schema.getTypes.size() == 2 &&
      schema.getTypes.get(0).getType == Schema.Type.NULL

  private case class Field(tpe: String, mode: String, fields: List[TableFieldSchema])
}

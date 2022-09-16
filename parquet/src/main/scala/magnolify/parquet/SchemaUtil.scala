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

import org.apache.avro.{Schema => AvroSchema}

import java.util

object SchemaUtil {

  type Path = String

  def deepCopy(
    schema: AvroSchema,
    rootDoc: Option[String],
    getFieldDoc: Path => Option[String]
  ): AvroSchema = deepCopyInternal(schema, rootDoc, getFieldDoc, "")

  private def deepCopyInternal(
    schema: AvroSchema,
    rootDoc: Option[String],
    getFieldDoc: Path => Option[String],
    path: String
  ): AvroSchema = {
    if (schema.isUnion) {
      val updatedSchemas = new util.ArrayList[AvroSchema]()
      schema.getTypes.forEach(x => updatedSchemas.add(deepCopyInternal(x, None, getFieldDoc, path)))
      return AvroSchema.createUnion(updatedSchemas)
    }

    if (schema.getType == AvroSchema.Type.ARRAY) {
      return AvroSchema.createArray(
        deepCopyInternal(schema.getElementType, None, getFieldDoc, path)
      )
    }

    if (schema.getType != AvroSchema.Type.RECORD) {
      return schema
    }

    val newFields = new java.util.ArrayList[AvroSchema.Field]()
    schema.getFields.forEach { oldField =>
      val newPath = if (path.isEmpty) oldField.name() else s"$path.${oldField.name()}"
      val field =
        new AvroSchema.Field(
          oldField.name(),
          deepCopyInternal(oldField.schema(), None, getFieldDoc, newPath),
          getFieldDoc(newPath).orNull,
          oldField
            .defaultVal()
        )
      newFields.add(field)
    }

    AvroSchema.createRecord(
      schema.getName,
      rootDoc.orNull,
      schema.getNamespace,
      schema.isError,
      newFields
    )
  }

}

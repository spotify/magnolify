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

import scala.jdk.CollectionConverters._

object SchemaUtil {

  type Path = String

  object Union {
    def unapply(schema: AvroSchema): Option[Seq[AvroSchema]] =
      if (schema.isUnion) Some(schema.getTypes.asScala.toSeq) else None
  }

  object Array {
    def unapply(schema: AvroSchema): Option[AvroSchema] =
      if (schema.getType == AvroSchema.Type.ARRAY) Some(schema.getElementType) else None
  }

  object Record {
    def unapply(schema: AvroSchema): Option[Seq[AvroSchema.Field]] =
      if (schema.getType == AvroSchema.Type.RECORD) Some(schema.getFields.asScala.toSeq) else None
  }

  def deepCopy(
    schema: AvroSchema,
    rootDoc: Option[String],
    getFieldDoc: Path => Option[String]
  ): AvroSchema = deepCopyInternal(schema, rootDoc, getFieldDoc, "")

  private def deepCopyInternal(
    schema: AvroSchema,
    rootDoc: Option[String],
    getFieldDoc: Path => Option[String],
    path: Path
  ): AvroSchema = schema match {
    case Union(ts) =>
      val updatedTypes = ts.foldLeft(List.newBuilder[AvroSchema]) { (b, t) =>
        b += deepCopyInternal(t, None, getFieldDoc, path)
      }
      AvroSchema.createUnion(updatedTypes.result().asJava)
    case Array(t) =>
      val updatedElementType = deepCopyInternal(t, None, getFieldDoc, path)
      AvroSchema.createArray(updatedElementType)
    case Record(fs) =>
      val updatedFields = fs.foldLeft(List.newBuilder[AvroSchema.Field]) { (b, f) =>
        val fieldPath = if (path.isEmpty) f.name() else s"$path.${f.name()}"
        b += new AvroSchema.Field(
          f.name(),
          deepCopyInternal(f.schema(), None, getFieldDoc, fieldPath),
          getFieldDoc(fieldPath).orNull,
          f.defaultVal()
        )
      }
      AvroSchema.createRecord(
        schema.getName,
        rootDoc.orNull,
        schema.getNamespace,
        schema.isError,
        updatedFields.result().asJava
      )
    case _ =>
      schema
  }
}

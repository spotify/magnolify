/*
 * Copyright 2021 Spotify AB
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

package magnolify.tools

import com.google.api.services.bigquery.model.{TableFieldSchema, TableSchema}

import scala.jdk.CollectionConverters._

object BigQueryParser extends SchemaParser[TableSchema] {
  override def parse(schema: TableSchema): Record =
    parseRecord(schema.getFields.asScala.toList)

  private def parseRecord(fields: List[TableFieldSchema]): Record = {
    val fs = fields.map { f =>
      val r = f.getMode match {
        case "REQUIRED" => Required
        case "NULLABLE" => Optional
        case "REPEATED" => Repeated
      }
      val s = f.getType match {
        case "INT64"     => Primitive.Long
        case "FLOAT64"   => Primitive.Double
        case "NUMERIC"   => Primitive.BigDecimal
        case "BOOL"      => Primitive.Boolean
        case "STRING"    => Primitive.String
        case "BYTES"     => Primitive.Bytes
        case "TIMESTAMP" => Primitive.Instant
        case "DATE"      => Primitive.LocalDate
        case "TIME"      => Primitive.LocalTime
        case "DATETIME"  => Primitive.LocalDateTime
        case "STRUCT"    => parseRecord(f.getFields.asScala.toList)
      }
      Field(f.getName, Option(f.getDescription), s, r)
    }
    Record(None, None, None, fs)
  }
}

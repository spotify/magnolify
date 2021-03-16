/*
 * Copyright 2021 Spotify AB.
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
package magnolify.tools

import magnolify.shims.JavaConverters._
import org.apache.avro
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema.Type

object AvroParser extends SchemaParser[avro.Schema] {
  override def parse(schema: avro.Schema): Record =
    parseRecord(schema)

  private def parseRecord(schema: avro.Schema): Record = {
    val fields = schema.getFields.asScala.iterator.map { f =>
      val (s, r) = parseSchemaAndRepetition(f.schema())
      Field(f.name(), Option(f.doc()), s, r)
    }.toList
    Record(Option(schema.getName), Option(schema.getNamespace), Option(schema.getDoc), fields)
  }

  private def parseEnum(schema: avro.Schema): Enum =
    Enum(
      Option(schema.getName),
      Option(schema.getNamespace),
      Option(schema.getDoc),
      schema.getEnumSymbols.asScala.toList
    )

  private def parseSchemaAndRepetition(schema: avro.Schema): (Schema, Repetition) =
    schema.getType match {
      case Type.UNION
          if schema.getTypes.size() == 2 &&
            schema.getTypes.asScala.count(_.getType == Type.NULL) == 1 =>
        val s = schema.getTypes.asScala.find(_.getType != Type.NULL).get
        if (s.getType == Type.ARRAY) {
          // Nullable array, e.g. ["null", {"type": "array", "items": ...}]
          (parseSchema(s.getElementType), Repeated)
        } else {
          (parseSchema(s), Optional)
        }
      case Type.ARRAY =>
        (parseSchema(schema.getElementType), Repeated)
      // FIXME: map
      case _ =>
        (parseSchema(schema), Required)
    }

  private def parseSchema(schema: avro.Schema): Schema = schema.getType match {
    // Nested types
    case Type.RECORD => parseRecord(schema)
    case Type.ENUM   => parseEnum(schema)

    // Logical types
    case Type.STRING if isLogical(schema, LogicalTypes.uuid().getName) =>
      Primitive.UUID
    case Type.BYTES if schema.getLogicalType.isInstanceOf[LogicalTypes.Decimal] =>
      Primitive.BigDecimal
    case Type.INT if schema.getLogicalType.isInstanceOf[LogicalTypes.Date] => Primitive.LocalDate

    // Millis
    case Type.LONG if schema.getLogicalType.isInstanceOf[LogicalTypes.TimestampMillis] =>
      Primitive.Instant
    case Type.INT if schema.getLogicalType.isInstanceOf[LogicalTypes.TimeMillis] =>
      Primitive.LocalTime
    // `LogicalTypes.LocalTimestampMillis` is Avro 1.10.0+
    case Type.LONG if isLogical(schema, "local-timestamp-millis") =>
      Primitive.LocalDateTime

    // Micros
    case Type.LONG if schema.getLogicalType.isInstanceOf[LogicalTypes.TimestampMicros] =>
      Primitive.Instant
    case Type.LONG if schema.getLogicalType.isInstanceOf[LogicalTypes.TimeMicros] =>
      Primitive.LocalTime
    // `LogicalTypes.LocalTimestampMicros` is Avro 1.10.0+
    case Type.LONG if isLogical(schema, "local-timestamp-micros") =>
      Primitive.LocalDateTime

    // BigQuery sqlType: DATETIME
    case Type.STRING if isLogical(schema, "datetime") => Primitive.LocalDateTime

    // Primitive types
    case Type.FIXED   => Primitive.Bytes
    case Type.STRING  => Primitive.String
    case Type.BYTES   => Primitive.Bytes
    case Type.INT     => Primitive.Int
    case Type.LONG    => Primitive.Long
    case Type.FLOAT   => Primitive.Float
    case Type.DOUBLE  => Primitive.Double
    case Type.BOOLEAN => Primitive.Boolean
    case Type.NULL    => Primitive.Unit

    case _ =>
      throw new IllegalArgumentException(s"Unsupported schema $schema")
  }

  private def isLogical(schema: avro.Schema, name: String): Boolean = {
    Option(schema.getLogicalType)
      .map(_.getName)
      .orElse(Option(schema.getProp("logicalType"))) // getLogicalType could return null
      .contains(name)
  }
}

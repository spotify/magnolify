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

import org.apache.parquet.schema.LogicalTypeAnnotation.{DecimalLogicalTypeAnnotation, TimeUnit}
import org.apache.parquet.schema.PrimitiveType.{PrimitiveTypeName => PTN}
import org.apache.parquet.schema.{
  GroupType,
  LogicalTypeAnnotation => LTA,
  MessageType,
  PrimitiveType,
  Type
}

import scala.jdk.CollectionConverters._

object ParquetParser extends SchemaParser[MessageType] {
  override def parse(schema: MessageType): Record = {
    val name = schema.getName
    val idx = name.lastIndexOf('.')
    val n = name.drop(idx + 1)
    parseRecord(schema.asGroupType()).copy(name = Some(n))
  }

  private def putRepetition(repetition: Type.Repetition)(schema: Schema): Schema =
    repetition match {
      case Type.Repetition.REQUIRED => schema
      case Type.Repetition.OPTIONAL => Optional(schema)
      case Type.Repetition.REPEATED => Repeated(schema)
    }

  private def parseRecord(groupType: GroupType): Record = {
    val fields = groupType.getFields.asScala.iterator.map { f =>
      Record.Field(f.getName, None, parseType(f))
    }.toList
    Record(None, None, fields)
  }

  private def isAvroArray(groupType: GroupType): Boolean =
    groupType.getLogicalTypeAnnotation == LTA.listType() &&
      groupType.getFieldCount == 1 &&
      groupType.getFieldName(0) == "array" &&
      groupType.getFields.get(0).isRepetition(Type.Repetition.REPEATED)
  private def parseAvroArray(groupType: GroupType): Schema =
    parseType(groupType.getFields.get(0))
  private def isMap(groupType: GroupType): Boolean =
    groupType.getLogicalTypeAnnotation == LTA.mapType() &&
      groupType.getFieldCount == 2 &&
      groupType.isRepetition(Type.Repetition.REPEATED)

  private def parseMap(groupType: GroupType): Schema = {
    val keySchema = parseType(groupType.getFields.get(0))
    val valueSchema = parseType(groupType.getFields.get(1))
    Mapped(keySchema, valueSchema)
  }

  private def parseType(tpe: Type): Schema =
    if (tpe.isPrimitive) {
      putRepetition(tpe.getRepetition)(parsePrimitive(tpe.asPrimitiveType()))
    } else {
      val groupType = tpe.asGroupType()
      if (isAvroArray(groupType)) {
        parseAvroArray(groupType)
      } else if (isMap(groupType)) {
        parseMap(groupType)
      } else {
        putRepetition(tpe.getRepetition)(parseRecord(groupType))
      }
    }

  private def parsePrimitive(primitiveType: PrimitiveType): Primitive = {
    val ptn = primitiveType.getPrimitiveTypeName
    val lta = primitiveType.getLogicalTypeAnnotation
    val decimal = lta match {
      case a: DecimalLogicalTypeAnnotation => Some(a)
      case _                               => None
    }

    ptn match {
      case PTN.BOOLEAN =>
        Primitive.Boolean

      // Signed 32-bit integers
      case PTN.INT32 if lta == null =>
        Primitive.Int
      case PTN.INT32 if lta == LTA.intType(8, true) =>
        Primitive.Byte
      case PTN.INT32 if lta == LTA.intType(16, true) =>
        Primitive.Short
      case PTN.INT32 if lta == LTA.intType(32, true) =>
        Primitive.Int

      // Signed 64-bit integers
      case PTN.INT64 if lta == null =>
        Primitive.Long
      case PTN.INT64 if lta == LTA.intType(64, true) =>
        Primitive.Long

      case PTN.FLOAT =>
        Primitive.Float
      case PTN.DOUBLE =>
        Primitive.Double

      case PTN.BINARY if lta == null =>
        Primitive.Bytes
      case PTN.BINARY if lta == LTA.stringType() || lta == LTA.enumType() =>
        Primitive.String

      // BigDecimal
      case PTN.INT32 if decimal.exists(a => 1 <= a.getPrecision && a.getPrecision <= 9) =>
        Primitive.BigDecimal
      case PTN.INT64 if decimal.exists(a => 1 <= a.getPrecision && a.getPrecision <= 18) =>
        Primitive.BigDecimal
      case PTN.FIXED_LEN_BYTE_ARRAY if decimal.nonEmpty =>
        Primitive.BigDecimal
      case PTN.BINARY if decimal.nonEmpty =>
        Primitive.BigDecimal

      // Date
      case PTN.INT32 if lta == LTA.dateType() =>
        Primitive.LocalDate

      // Millis
      case PTN.INT64 if lta == LTA.timestampType(true, TimeUnit.MILLIS) =>
        Primitive.Instant
      case PTN.INT64 if lta == LTA.timestampType(false, TimeUnit.MILLIS) =>
        Primitive.LocalDateTime
      case PTN.INT32 if lta == LTA.timeType(true, TimeUnit.MILLIS) =>
        Primitive.OffsetTime
      case PTN.INT32 if lta == LTA.timeType(false, TimeUnit.MILLIS) =>
        Primitive.LocalTime

      // Micros
      case PTN.INT64 if lta == LTA.timestampType(true, TimeUnit.MICROS) =>
        Primitive.Instant
      case PTN.INT64 if lta == LTA.timestampType(false, TimeUnit.MICROS) =>
        Primitive.LocalDateTime
      case PTN.INT64 if lta == LTA.timeType(true, TimeUnit.MICROS) =>
        Primitive.OffsetTime
      case PTN.INT64 if lta == LTA.timeType(false, TimeUnit.MICROS) =>
        Primitive.LocalTime

      // Nanos
      case PTN.INT64 if lta == LTA.timestampType(true, TimeUnit.NANOS) =>
        Primitive.Instant
      case PTN.INT64 if lta == LTA.timestampType(false, TimeUnit.NANOS) =>
        Primitive.LocalDateTime
      case PTN.INT64 if lta == LTA.timeType(true, TimeUnit.NANOS) =>
        Primitive.OffsetTime
      case PTN.INT64 if lta == LTA.timeType(false, TimeUnit.NANOS) =>
        Primitive.LocalTime

      case _ =>
        throw new IllegalArgumentException(s"Unsupported primitive type $primitiveType")
    }
  }
}

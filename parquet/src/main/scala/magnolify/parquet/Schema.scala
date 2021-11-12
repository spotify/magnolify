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
package magnolify.parquet

import magnolify.shims.JavaConverters._
import org.apache.parquet.io.InvalidRecordException
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type.Repetition
import org.apache.parquet.schema.{
  GroupType,
  LogicalTypeAnnotation,
  MessageType,
  PrimitiveType,
  Type,
  Types
}

private object Schema {
  def rename(schema: Type, name: String): Type = {
    if (schema.isPrimitive) {
      val p = schema.asPrimitiveType()
      Types
        .primitive(p.getPrimitiveTypeName, schema.getRepetition)
        .length(p.getTypeLength)
        .as(schema.getLogicalTypeAnnotation)
        .named(name)
    } else {
      schema
        .asGroupType()
        .getFields
        .asScala
        .foldLeft(Types.buildGroup(schema.getRepetition))(_.addField(_))
        .as(schema.getLogicalTypeAnnotation)
        .named(name)
    }
  }

  def setRepetition(schema: Type, repetition: Repetition): Type = {
    require(schema.isRepetition(Repetition.REQUIRED))
    if (schema.isPrimitive) {
      Types
        .primitive(schema.asPrimitiveType().getPrimitiveTypeName, repetition)
        .as(schema.getLogicalTypeAnnotation)
        .named(schema.getName)
    } else {
      schema
        .asGroupType()
        .getFields
        .asScala
        .foldLeft(Types.buildGroup(repetition))(_.addField(_))
        .as(schema.getLogicalTypeAnnotation)
        .named(schema.getName)
    }
  }

  def setLogicalType(schema: Type, lta: LogicalTypeAnnotation): Type = {
    require(schema.isPrimitive)
    Types
      .primitive(schema.asPrimitiveType().getPrimitiveTypeName, schema.getRepetition)
      .as(lta)
      .named(schema.getName)
  }

  def primitive(ptn: PrimitiveTypeName, lta: LogicalTypeAnnotation = null, length: Int = 0): Type =
    Types.required(ptn).as(lta).length(length).named(ptn.name())

  def message(schema: Type): MessageType = {
    val builder = Types.buildMessage()
    schema.asGroupType().getFields.asScala.foreach(builder.addField)
    builder.named(schema.getName)
  }

  def checkCompatibility(writer: Type, reader: Type): Unit = {
    def isRepetitionBackwardCompatible(w: Type, r: Type) =
      (w.getRepetition, r.getRepetition) match {
        case (Repetition.REQUIRED, Repetition.OPTIONAL) => true
        case (r1, r2)                                   => r1 == r2
      }

    if (
      !isRepetitionBackwardCompatible(writer, reader) ||
      writer.isPrimitive != reader.isPrimitive
    ) {
      throw new InvalidRecordException(s"$writer found: expected $reader")
    }

    writer match {
      case _: GroupType =>
        val wg = writer.asGroupType()
        val rg = reader.asGroupType()
        rg.getFields.asScala.foreach { rf =>
          if (wg.containsField(rf.getName)) {
            val wf = wg.getType(rf.getName)
            checkCompatibility(wf, rf)
          } else {
            if (
              rf.isRepetition(Repetition.REQUIRED) &&
              rf.getLogicalTypeAnnotation != LogicalTypeAnnotation.listType()
            ) {
              throw new InvalidRecordException(
                s"${rf.getRepetition} field ${rf.getName} missing in file schema"
              )
            }
          }
        }
      case _: PrimitiveType =>
        val wf = writer.asPrimitiveType()
        val rf = reader.asPrimitiveType()
        if (wf.getPrimitiveTypeName != rf.getPrimitiveTypeName) {
          throw new InvalidRecordException(s"$rf found: expected $wf")
        }
    }
  }
}

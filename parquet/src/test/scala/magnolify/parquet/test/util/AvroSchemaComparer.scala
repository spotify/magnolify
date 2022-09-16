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

package magnolify.parquet.test.util

import org.apache.avro.Schema
import cats.implicits._
import scala.jdk.CollectionConverters._

trait AvroSchemaComparer {

  def compareRecordSchemas(s1: Schema, s2: Schema): List[String]

  def compareOtherSchemas(s1: Schema, s2: Schema): List[String]

  def compareFields(s1: Schema.Field, s2: Schema.Field): List[String]

  private def tryToCompareUnions(s1: Schema, s2: Schema): Option[List[String]] = {
    if (s1.isUnion && s2.isUnion) {
      if (s1.getTypes.size() != s2.getTypes.size()) {
        List(s"Size of union is different ${s1.getTypes.size()} != ${s2.getTypes.size()}").some
      } else {
        val unionTypes = s1.getTypes.asScala.zip(s2.getTypes.asScala)
        unionTypes.flatMap { case (is1, is2) => compareEntireSchemas(is1, is2) }.toList.some
      }
    } else if (s1.isUnion || s2.isUnion) {
      List("Only one type is union").some
    } else {
      none
    }
  }

  private def tryToCompareArrays(s1: Schema, s2: Schema): Option[List[String]] = {
    if (s1.getType == Schema.Type.ARRAY && s2.getType == Schema.Type.ARRAY) {
      compareEntireSchemas(s1.getElementType, s2.getElementType).some
    } else if (s1.getType == Schema.Type.ARRAY || s2.getType == Schema.Type.ARRAY) {
      List("Only one type is array").some
    } else {
      none
    }
  }

  private def tryToCompareRecords(s1: Schema, s2: Schema): Option[List[String]] = {
    if (s1.getType == Schema.Type.RECORD && s2.getType == Schema.Type.RECORD) {
      val fields1 = s1.getFields.asScala.map(_.name()).toSet
      val fields2 = s2.getFields.asScala.map(_.name()).toSet

      val fieldsEqualResults =
        if (fields1.equals(fields2)) List()
        else
          List(s"Field set is not equal $fields1 != $fields2")

      val results = fieldsEqualResults ++
        compareRecordSchemas(s1, s2) ++
        fields1
          .intersect(fields2)
          .toList
          .flatMap { field =>
            val field1 = s1.getField(field)
            val field2 = s2.getField(field)
            compareFields(field1, field2) ++ compareEntireSchemas(field1.schema(), field2.schema())
          }
      results.some
    } else {
      none
    }
  }

  def compareEntireSchemas(
    schema1: Schema,
    schema2: Schema
  ): List[String] = {
    if (schema1 == null && schema2 == null) {
      List()
    } else if (schema1 == null && schema2 != null) {
      List("schema1 is null")
    } else if (schema1 != null && schema2 == null) {
      List("schema2 is null")
    } else {
      tryToCompareUnions(schema1, schema2) match {
        case Some(results) => results
        case None =>
          tryToCompareArrays(schema1, schema2) match {
            case Some(results) => results
            case None =>
              tryToCompareRecords(schema1, schema2) match {
                case Some(results) => results
                case None =>
                  compareOtherSchemas(schema1, schema2)
              }
          }
      }
    }
  }
}

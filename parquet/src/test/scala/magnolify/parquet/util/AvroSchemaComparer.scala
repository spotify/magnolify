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

package magnolify.parquet.util

import magnolify.parquet.SchemaUtil._
import org.apache.avro.Schema
import scala.util.Try

object AvroSchemaComparer {

  def compareSchemas(
    schema1: Schema,
    schema2: Schema,
    path: String = "root"
  ): List[String] = {
    compareBasicTypeInfo(schema1, schema2, path) ++ {
      (schema1, schema2) match {
        case (null, null) => List()
        case (null, _)    => List("schema2 is null")
        case (_, null)    => List("schema1 is null")
        case (Union(nestedTypes1), Union(nestedTypes2)) =>
          if (nestedTypes1.size != nestedTypes2.size) {
            List(s"Size of union is different ${nestedTypes1.size} != ${nestedTypes2.size}")
          } else {
            nestedTypes1
              .zip(nestedTypes2)
              .flatMap { case (is1, is2) => compareSchemas(is1, is2, path) }
          }
        case (Array(arrayEl1), Array(arrayEl2)) =>
          compareSchemas(arrayEl1, arrayEl2, path)
        case (Record(schemaFields1), Record(schemaFields2)) =>
          val fields1 = schemaFields1.map(_.name())
          val fields2 = schemaFields2.map(_.name())

          val fieldsEqualResults = require(
            fields1 == fields2,
            s"$path fields are not equal '$fields1' != '$fields2'"
          )

          fieldsEqualResults ++
            fields1
              .intersect(fields2)
              .flatMap { f =>
                val field1 = schema1.getField(f)
                val field2 = schema2.getField(f)

                require(
                  field1.doc() == field2.doc(),
                  s"$path.$f field 'doc' are different '${field1.doc}' != '${field2.doc}'"
                ) ++ require(
                  field1.pos() == field2.pos(),
                  s"$path.$f field 'pos' are different '${field1.pos}' != '${field2.pos}'"
                ) ++ compareSchemas(field1.schema(), field2.schema(), s"$path.$f")
              }
        case _ =>
          List.empty
      }
    }
  }

  private def require(condition: Boolean, error: => String): Option[String] = {
    if (!condition)
      Some(error)
    else None
  }

  private def compareBasicTypeInfo(s1: Schema, s2: Schema, path: String): List[String] = {
    if (s1 != null && s2 != null) {
      require(
        s1.getName == s2.getName,
        s"$path 'name' are different '${s1.getName}' != '${s2.getName}'"
      ) ++ require(
        s1.getType == s2.getType,
        s"$path 'type' are different '${s1.getType}' != '${s2.getType}'"
      ) ++ require(
        s1.isNullable == s2.isNullable,
        s"$path 'isNullable' are different '${s1.isNullable}' != '${s2.isNullable}'"
      ) ++ require(
        s1.getDoc == s2.getDoc,
        s"$path 'doc' are different '${s1.getDoc}' != '${s2.getDoc}'"
      ) ++ require(
        Try(s1.getNamespace == s2.getNamespace).getOrElse(true),
        s"$path 'namespace' are different '${s1.getNamespace}' != '${s2.getNamespace}'"
      )
    }.toList
    else List.empty
  }
}

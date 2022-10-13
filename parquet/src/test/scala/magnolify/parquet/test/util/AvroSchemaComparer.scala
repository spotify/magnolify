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
import magnolify.parquet.SchemaUtil._

object AvroSchemaComparer {

  def compareSchemas(
    schema1: Schema,
    schema2: Schema,
    path: String = "root"
  ): Seq[String] = (schema1, schema2) match {
    case (null, null) =>
      Seq.empty
    case (null, _) =>
      Seq(s"left $path schema is null")
    case (_, null) =>
      Seq(s"right $path schema is null")
    case (Union(ts1), Union(ts2)) =>
      if (ts1.size != ts2.size) {
        Seq(s"$path union of different sizes ${ts1.size} != ${ts2.size}")
      } else {
        ts1.zip(ts2).flatMap { case (l, r) => compareSchemas(l, r, path) }
      }
    case (Array(t1), Array(t2)) =>
      compareSchemas(t1, t2, path)
    case (Record(fs1), Record(fs2)) =>
      val ns1 = fs1.map(_.name)
      val ns2 = fs2.map(_.name)
      if (ns1 != ns2) {
        Seq(s"$path record have different field names $ns1 != $ns2")
      } else {
        val d1 = schema1.getDoc()
        val d2 = schema2.getDoc()
        val docError =
          if (d1 != d2) Some(s"$path record docs are not equal '$d1' != '$d2'") else None
        docError.toSeq ++ ns1.flatMap { n =>
          val p = s"$path.$n"
          val f1 = schema1.getField(n)
          val f2 = schema2.getField(n)
          val fDocError =
            if (f1.doc != f2.doc) Some(s"$p field docs are not equal '${f1.doc}' != '${f2.doc}'")
            else None
          fDocError ++ compareSchemas(f1.schema, f2.schema, p)
        }
      }
    case _ =>
      if (schema1.getType == schema2.getType) {
        Seq.empty
      } else {
        Seq(s"$path schema types are not equal ${schema1.getType} != ${schema2.getType}")
      }
  }
}

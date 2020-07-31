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

import java.{util => ju}

import com.google.api.services.bigquery.model.{TableFieldSchema, TableSchema}
import magnolify.shared.CaseMapper
import magnolify.shims.JavaConverters._

object TableRowGen {

  /**
   * Parameters for [[TableRowGen]].
   * @param caseMapper mapping from BigQuery to Scala case format.
   * @param repeatedType collection type for repeated fields.
   * @param typeOverrides mapping overrides from BigQuery to Scala types, e.g.
   *                      `"BYTES" -> "ByteString"`.
   * @param extraImports extra imports for the generated files.
   */
  case class Params(
    caseMapper: CaseMapper,
    repeatedType: String,
    typeOverrides: Map[String, String],
    extraImports: List[String]
  ) {
    private val baseImports: List[String] = List("org.joda.time._", "magnolify.bigquery._")

    val imports: List[String] = (baseImports ++ extraImports).distinct.sorted
  }

  object Params {
    val default: Params = Params(CaseMapper.identity, "List", Map.empty, Nil)
  }

  /**
   * Generate a single [[Module]] file for a given BigQuery [[TableSchema]].
   *
   * Nested `STRUCT`s are mapped to nested case classes in the companion object.
   */
  def gen(
    schema: TableSchema,
    name: String,
    namespace: String,
    description: Option[String] = None,
    params: Params = Params.default
  ): Module =
    Module(
      namespace,
      params.imports,
      toCaseClass(name, schema.getFields, params).copy(annotations = toAnnotations(description))
    )

  private val typeMap: Map[String, String] = Map(
    "BOOL" -> "Boolean",
    "INT64" -> "Long",
    "FLOAT64" -> "Double",
    "STRING" -> "String",
    "NUMERIC" -> "BigDecimal",
    "BYTES" -> "Array[Byte]",
    "TIMESTAMP" -> "Instant",
    "DATE" -> "LocalDate",
    "TIME" -> "LocalTime",
    "DATETIME" -> "LocalDateTime"
  )

  private val modeMap: Map[String, Repetition] = Map(
    "REQUIRED" -> Repetition.Required,
    "NULLABLE" -> Repetition.Nullable,
    "REPEATED" -> Repetition.Repeated
  )

  private def toCaseClass(
    name: String,
    fields: ju.List[TableFieldSchema],
    params: Params
  ): CaseClass = {
    val fb = List.newBuilder[Field]
    val nb = List.newBuilder[CaseClass]
    fields.asScala.foreach { f =>
      val (rawType, nested) = toScalaType(name, f, params)
      val tpe = modeMap.get(f.getMode) match {
        case Some(Repetition.Required) => rawType
        case Some(Repetition.Nullable) => s"Option[$rawType]"
        case Some(Repetition.Repeated) => s"${params.repeatedType}[$rawType]"
        case None                      => throw new IllegalArgumentException(s"Unsupported mode: ${f.getMode}")
      }
      fb += Field(
        params.caseMapper.map(f.getName),
        tpe,
        toAnnotations(Option(f.getDescription)),
        None
      )
      nb ++= nested
    }
    // FIXME: de-dup nested classes
    val companion = nb.result() match {
      case Nil => None
      case xs  => Some(Companion(name, xs))
    }
    CaseClass(name, fb.result(), Nil, companion)
  }

  private def toScalaType(
    name: String,
    schema: TableFieldSchema,
    params: Params
  ): (String, Option[CaseClass]) =
    if (schema.getType == "STRUCT") {
      val label = Names.toUpperCamel(schema.getName)
      (s"$name.$label", Some(toCaseClass(label, schema.getFields, params)))
    } else {
      (typeMap ++ params.typeOverrides).get(schema.getType) match {
        case Some(tpe) => (tpe, None)
        case None =>
          throw new IllegalArgumentException(s"Unsupported table field type: ${schema.getType}")
      }
    }

  private def toAnnotations(description: Option[String]): List[String] =
    description.map(d => s"""@description("$d")""").toList
}

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

import magnolify.shared.CaseMapper
import magnolify.shims.JavaConverters._
import org.apache.avro.Schema

object AvroGen {

  /**
   * Parameters for [[AvroGen]].
   * @param caseMapper mapping from Avro to Scala case format.
   * @param fallbackNamespace fallback for the Avro schema namespace.
   * @param relocateNamespace relocate function for Avro schema namespace.
   * @param arrayType collection type for `ARRAY` fields.
   * @param mapType collection type for `MAP` fields.
   * @param typeOverrides mapping overrides from Avro to Scala types, e.g. `BYTES -> "ByteString"`.
   * @param extraImports extra imports for the generated files.
   */
  case class Params(
    caseMapper: CaseMapper,
    fallbackNamespace: Option[String],
    relocateNamespace: String => String,
    arrayType: String,
    mapType: String,
    typeOverrides: Map[Schema.Type, String],
    extraImports: List[String]
  ) {
    private val baseImports: List[String] = List("magnolify.avro._")

    val imports: List[String] = (baseImports ++ extraImports).distinct.sorted
  }

  object Params {
    val default: Params = Params(CaseMapper.identity, None, identity, "List", "Map", Map.empty, Nil)
  }

  /**
   * Generate [[Module]] files for a given Avro [[Schema]].
   *
   * Nested `RECORD`s are mapped to separate [[Module]]s.
   */
  def gen(
    schema: Schema,
    params: Params = Params.default
  ): List[Module] = toModules(schema, params)

  // FIXME: support logical types
  private val typeMap: Map[Schema.Type, String] = Map(
    Schema.Type.STRING -> "String",
    Schema.Type.BYTES -> "Array[Byte]",
    Schema.Type.INT -> "Int",
    Schema.Type.LONG -> "Long",
    Schema.Type.FLOAT -> "Float",
    Schema.Type.DOUBLE -> "Double",
    Schema.Type.BOOLEAN -> "Boolean",
    Schema.Type.NULL -> "Unit"
  )

  private def toModules(
    schema: Schema,
    params: Params
  ): List[Module] = {
    val fb = List.newBuilder[Field]
    val nb = List.newBuilder[Module]
    schema.getFields.asScala.foreach { f =>
      val annotations = Option(f.doc()).map(d => s"""@doc("$d")""").toList
      val (tpe, nested) = toScalaType(f.schema(), params)
      fb += Field(params.caseMapper.map(f.name()), tpe, annotations, None)
      nb ++= nested
    }
    val annotations = Option(schema.getDoc).map(d => s"""@doc("$d")""").toList
    val cc = CaseClass(schema.getName, fb.result(), annotations, None)
    val modules = Module(getNamespace(schema, params), params.imports, cc) :: nb.result()
    Module.distinct(modules)
  }

  private def toScalaType(schema: Schema, params: Params): (String, Seq[Module]) =
    if (schema.getType == Schema.Type.RECORD) {
      val namespace = getNamespace(schema, params)
      (s"$namespace.${schema.getName}", toModules(schema, params))
    } else if (schema.getType == Schema.Type.ARRAY) {
      val (rawType, nested) = toScalaType(schema.getElementType, params)
      (s"${params.arrayType}[$rawType]", nested)
    } else if (schema.getType == Schema.Type.MAP) {
      val (rawType, nested) = toScalaType(schema.getValueType, params)
      (s"${params.mapType}[String, $rawType]", nested)
    } else if (isNullable(schema)) {
      val (rawType, nested) = toScalaType(schema.getTypes.get(1), params)
      (s"Option[$rawType]", nested)
    } else {
      (typeMap ++ params.typeOverrides).get(schema.getType) match {
        case Some(tpe) => (tpe, Nil)
        case None =>
          throw new IllegalArgumentException(s"Unsupported type: ${schema.getType}")
      }
    }

  private def isNullable(schema: Schema): Boolean =
    schema.getType == Schema.Type.UNION &&
      schema.getTypes.size() == 2 &&
      schema.getTypes.get(0).getType == Schema.Type.NULL

  private def getNamespace(schema: Schema, params: Params): String =
    (Option(schema.getNamespace), params.fallbackNamespace) match {
      case (Some(a), _)    => params.relocateNamespace(a)
      case (None, Some(b)) => b
      case _ =>
        throw new IllegalArgumentException(s"Missing namespace: ${schema.getName}")
    }
}

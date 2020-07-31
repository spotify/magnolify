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
package magnolify.codegen.avro

import java.nio.file.Path

import com.google.common.base.CaseFormat
import magnolify.avro._
import magnolify.codegen._
import magnolify.shared.CaseMapper
import org.apache.avro.Schema

class AvroFixtures(val srcDir: Path, val rsrcDir: Path) extends BaseFixtures {
  def gen(): Unit = {
    gen[Required]()
    gen[Nullable]()
    gen[Repeated]()
    gen[Maps]()
    gen[Outer]()
    gen[Annotations]()

    {
      // Scala `lowerCamel` to generated BigQuery `lower_underscore`
      val c = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE)
      implicit val at: AvroType[CaseMapping] = AvroType[CaseMapping](CaseMapper(c.convert))
      // Reverse BigQuery `lower_underscore` to generated Scala `lowerCamel`
      gen[CaseMapping](params =
        AvroGen.Params.default.copy(caseMapper = CaseMapper(c.reverse().convert))
      )
    }

    gen[ArrayType](params =
      AvroGen.Params.default
        .copy(arrayType = "mutable.Buffer", extraImports = List("scala.collection.mutable"))
    )

    gen[MapType](params =
      AvroGen.Params.default
        .copy(mapType = "mutable.Map", extraImports = List("scala.collection.mutable"))
    )

    gen[TypeOverrides](params =
      AvroGen.Params.default.copy(
        typeOverrides = Map(Schema.Type.BYTES -> "ByteString"),
        extraImports = List("com.google.protobuf.ByteString")
      )
    )
  }

  def gen[T](
    params: AvroGen.Params = AvroGen.Params.default
  )(implicit at: AvroType[T]): Unit = {
    val modules = AvroGen.gen(at.schema, params)
    modules.foreach(_.saveTo(srcDir))

    val schemaString = at.schema.toString(true)
    saveSchema(rsrcDir, at.schema.getName, at.schema.getNamespace, schemaString, ".json")
  }
}

case class Required(
  s: String,
  ba: Array[Byte],
  i: Int,
  l: Long,
  f: Float,
  d: Double,
  b: Boolean,
  u: Unit
)
case class Nullable(b: Option[Boolean], l: Option[Long], inner: Option[Inner])
case class Repeated(b: List[Boolean], l: List[Long], inner: List[Inner])
case class Maps(b: Map[String, Boolean], l: Map[String, Long], inner: Map[String, Inner])

case class Outer(b: Boolean, l: Long, i1: Inner, i2: Option[Inner], i3: List[Inner])
case class Inner(b: Boolean, l: Long)

@doc("record doc")
case class Annotations(@doc("boolean doc") b: Boolean, @doc("long doc") l: Long)

case class CaseMapping(booleanField: Boolean, longField: Long)

case class ArrayType(b: List[Boolean], l: List[Long])
case class MapType(b: Map[String, Boolean], l: Map[String, Long])
case class TypeOverrides(ba: Array[Byte])

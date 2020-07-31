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
package magnolify.codegen.bigquery

import java.nio.file.Path

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.common.base.CaseFormat
import magnolify.bigquery._
import magnolify.codegen._
import magnolify.shared.CaseMapper
import org.joda.time._

import scala.reflect.ClassTag

class TableRowFixtures(val srcDir: Path, val rsrcDir: Path) extends BaseFixtures {
  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

  def gen(): Unit = {
    gen[Required]()
    gen[Nullable]()
    gen[Repeated]()
    gen[Outer]()
    gen[Annotations]()

    {
      // Scala `lowerCamel` to generated BigQuery `lower_underscore`
      val c = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE)
      implicit val trt: TableRowType[CaseMapping] = TableRowType[CaseMapping](CaseMapper(c.convert))
      // Reverse BigQuery `lower_underscore` to generated Scala `lowerCamel`
      gen[CaseMapping](params =
        TableRowGen.Params.default.copy(caseMapper = CaseMapper(c.reverse().convert))
      )
    }

    gen[RepeatedType](params =
      TableRowGen.Params.default
        .copy(repeatedType = "mutable.Buffer", extraImports = List("scala.collection.mutable"))
    )

    gen[TypeOverrides](params =
      TableRowGen.Params.default.copy(
        typeOverrides = Map("BYTES" -> "ByteString"),
        extraImports = List("com.google.protobuf.ByteString")
      )
    )
  }

  def gen[T: ClassTag](
    params: TableRowGen.Params = TableRowGen.Params.default
  )(implicit trt: TableRowType[T]): Unit = {
    val module = TableRowGen.gen(trt.schema, name[T], namespace, Option(trt.description), params)
    module.saveTo(srcDir)

    val schemaString = mapper.writeValueAsString(trt.schema)
    saveSchema(rsrcDir, module.caseClass.name, module.namespace, schemaString, ".json")
  }
}

case class Required(
  b: Boolean,
  l: Long,
  f: Double,
  s: String,
  n: BigDecimal,
  ba: Array[Byte],
  ts: Instant,
  date: LocalDate,
  time: LocalTime,
  dt: LocalDateTime
)
case class Nullable(b: Option[Boolean], l: Option[Long], inner: Option[Inner])
case class Repeated(b: List[Boolean], l: List[Long], inner: List[Inner])

case class Outer(b: Boolean, l: Long, i1: Inner, i2: Option[Inner], i3: List[Inner])
case class Inner(b: Boolean, l: Long)

@description("table description")
case class Annotations(
  @description("boolean description") b: Boolean,
  @description("long description") l: Long
)

case class CaseMapping(booleanField: Boolean, longField: Long)

case class RepeatedType(b: List[Boolean], l: List[Long])
case class TypeOverrides(ba: Array[Byte])

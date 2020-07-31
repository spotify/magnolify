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

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.api.services.bigquery.model.TableSchema
import com.google.common.base.CaseFormat
import com.google.protobuf.ByteString
import magnolify.bigquery._
import magnolify.codegen._
import magnolify.shared.CaseMapper

import scala.reflect.ClassTag

class TableRowGenSuite extends BaseGenSuite {
  private val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

  private def test[T: ClassTag](implicit trt: TableRowType[T]): Unit = test(name[T]) {
    // FIXME: `TableSchema` has wonky equality, round-trip seems to fix it
    val actual = mapper.readValue(mapper.writeValueAsString(trt.schema), classOf[TableSchema])
    val expected = mapper.readValue(getSchema[T](".json"), classOf[TableSchema])
    assertEquals(actual, expected)
  }

  test[Required]
  test[Nullable]
  test[Repeated]
  test[Outer]
  test[Annotations]

  test("table description") {
    assertEquals(TableRowType[Annotations].description, "table description")
  }

  {
    val c = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE)
    implicit val trt: TableRowType[CaseMapping] = TableRowType[CaseMapping](CaseMapper(c.convert))
    test[CaseMapping]
  }

  test[RepeatedType]

  {
    implicit val trfByteString: TableRowField[ByteString] =
      TableRowField.from[Array[Byte]](ByteString.copyFrom)(_.toByteArray)
    test[TypeOverrides]
  }
}

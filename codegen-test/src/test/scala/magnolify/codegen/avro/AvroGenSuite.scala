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

import com.google.common.base.CaseFormat
import com.google.protobuf.ByteString
import magnolify.avro._
import magnolify.codegen._
import magnolify.shared.CaseMapper
import org.apache.avro.Schema

import scala.collection.mutable
import scala.reflect.ClassTag

class AvroGenSuite extends BaseGenSuite {
  private def test[T: ClassTag](implicit at: AvroType[T]): Unit = test(name[T]) {
    val actual = at.schema
    val expected = new Schema.Parser().parse(getSchema[T](".json"))
    assertEquals(actual, expected)
  }

  test[Required]
  test[Nullable]
  test[Repeated]
  test[Maps]
  test[Outer]
  test[Annotations]

  {
    val c = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE)
    implicit val at: AvroType[CaseMapping] = AvroType[CaseMapping](CaseMapper(c.convert))
    test[CaseMapping]
  }

  test[ArrayType]

  {
    implicit def afTrieMap[V: AvroField]: AvroField[mutable.Map[String, V]] =
      AvroField.from[Map[String, V]](m => mutable.Map(m.toSeq: _*))(_.toMap)
    test[MapType]
  }

  {
    implicit val afByteString: AvroField[ByteString] =
      AvroField.from[Array[Byte]](ByteString.copyFrom)(_.toByteArray)
    test[TypeOverrides]
  }
}

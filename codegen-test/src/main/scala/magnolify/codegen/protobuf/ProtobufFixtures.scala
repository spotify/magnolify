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
package magnolify.codegen.protobuf

import java.nio.file.Path

import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Message
import magnolify.codegen._
import magnolify.shared.CaseMapper
import magnolify.test._

import scala.reflect.ClassTag

class ProtobufFixtures(val srcDir: Path, val rsrcDir: Path) extends BaseFixtures {
  private def relocateTo(to: String): String => String = from => {
    require(from == "magnolify.test")
    to
  }

  private val params: ProtobufGen.Params =
    ProtobufGen.Params.default.copy(relocateNamespace = relocateTo("magnolify.codegen.protobuf"))

  def gen(): Unit = {
    gen[Proto2.NumbersP2]()
    gen[Proto2.RequiredP2]()
    gen[Proto2.NullableP2]()
    gen[Proto2.RepeatedP2]()
    gen[Proto2.NestedP2]()

    gen[Proto3.NumbersP3]()
    gen[Proto3.SingularP3]()
    gen[Proto3.RepeatedP3]()
    gen[Proto3.NestedP3]()
    gen[Proto3.BytesP3]()

    gen[Proto3.UpperCase](params = params.copy(caseMapper = CaseMapper(_.toLowerCase)))

    gen[Proto3.SingularP3](
      params.copy(
        relocateNamespace = relocateTo("magnolify.codegen.protobuf.proto3option"),
        proto3Option = true
      )
    )

    gen[Proto3.RepeatedP3](
      params.copy(
        relocateNamespace = relocateTo("magnolify.codegen.protobuf.repeated"),
        repeatedType = "mutable.Buffer",
        extraImports = List("scala.collection.mutable")
      )
    )

    gen[Proto3.SingularP3](
      params.copy(
        relocateNamespace = relocateTo("magnolify.codegen.protobuf.overrides"),
        typeOverrides = Map(FieldDescriptor.Type.STRING -> "URI"),
        extraImports = List("java.net.URI")
      )
    )
  }

  def gen[T <: Message](params: ProtobufGen.Params = params)(implicit ct: ClassTag[T]): Unit = {
    val descriptor = ProtobufClient.fromClass(ct.runtimeClass.getName)
    val modules = ProtobufGen.gen(descriptor, params)
    modules.foreach(_.saveTo(srcDir))
  }
}

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

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.FileDescriptor.Syntax
import com.google.protobuf.Descriptors.{Descriptor, FieldDescriptor, FileDescriptor}
import com.google.protobuf.Message
import magnolify.shared.CaseMapper
import magnolify.shims.JavaConverters._

import scala.reflect.ClassTag

object ProtobufGen {

  /**
   * Parameters for [[ProtobufGen]].
   * @param caseMapper mapping from Protobuf to Scala case format.
   * @param fallbackNamespace fallback for the Protobuf message namespace.
   * @param relocateNamespace relocate function for Protobuf message namespace.
   * @param proto3Option generation PROTO3 singular fields as `Option[T]`.
   * @param repeatedType collection type for repeated fields.
   * @param typeOverrides mapping overrides for Protobuf to Scala types, e.g.
   *                      `BYTESTRING -> "Array[Byte]"`.
   * @param extraImports extra imports for the generated files.
   */
  case class Params(
    caseMapper: CaseMapper,
    fallbackNamespace: Option[String],
    relocateNamespace: String => String,
    proto3Option: Boolean,
    repeatedType: String,
    typeOverrides: Map[FieldDescriptor.Type, String],
    extraImports: List[String]
  ) {
    private val baseImports: List[String] =
      List("magnolify.protobuf._", "com.google.protobuf.ByteString")

    val imports: List[String] = (baseImports ++ extraImports).distinct.sorted
  }

  object Params {
    val default: Params = Params(CaseMapper.identity, None, identity, false, "List", Map.empty, Nil)
  }

  /**
   * Generate [[Module]] files for a given Protobuf [[Descriptor]].
   *
   * Nested messages are mapped to separate [[Module]]s.
   */
  def gen(descriptor: Descriptor, params: Params = Params.default): List[Module] =
    toModules(descriptor, params)

  private val typeMap: Map[FieldDescriptor.Type, String] = FieldDescriptor.Type
    .values()
    .collect { t =>
      t.getJavaType match {
        case FieldDescriptor.JavaType.INT         => t -> "Int"
        case FieldDescriptor.JavaType.LONG        => t -> "Long"
        case FieldDescriptor.JavaType.FLOAT       => t -> "Float"
        case FieldDescriptor.JavaType.DOUBLE      => t -> "Double"
        case FieldDescriptor.JavaType.BOOLEAN     => t -> "Boolean"
        case FieldDescriptor.JavaType.STRING      => t -> "String"
        case FieldDescriptor.JavaType.BYTE_STRING => t -> "ByteString"
      }
    }
    .toMap

  private def toModules(msg: Descriptor, params: Params): List[Module] = {
    val fb = List.newBuilder[Field]
    val nb = List.newBuilder[Module]
    val syntax = msg.getFile.getSyntax
    msg.getFields.asScala.foreach { f =>
      val (rawType, nested) = toScalaType(f, params)
      val tpe = if (f.isRepeated) {
        s"${params.repeatedType}[$rawType]"
      } else if (
        (syntax == Syntax.PROTO2 && f.isOptional) || (syntax == Syntax.PROTO3 && params.proto3Option)
      ) {
        s"Option[$rawType]"
      } else {
        rawType
      }
      fb += Field(params.caseMapper.map(f.getName), tpe, Nil, None)
      nb ++= nested
    }
    val cc = CaseClass(msg.getName, fb.result(), Nil, None)
    // FIXME: de-dup nested classes
    Module(getNamespace(msg, params), params.imports, cc) :: nb.result()
  }

  private def toScalaType(field: FieldDescriptor, params: Params): (String, List[Module]) =
    if (field.getType == FieldDescriptor.Type.MESSAGE) {
      val ns = getNamespace(field.getMessageType, params)
      val tpe = s"$ns.${field.getMessageType.getName}"
      (tpe, toModules(field.getMessageType, params))
    } else {
      (typeMap ++ params.typeOverrides).get(field.getType) match {
        case Some(tpe) => (tpe, Nil)
        case None      => throw new IllegalArgumentException(s"Unsupported field type: ${field.getType}")
      }
    }

  private def getNamespace(msg: Descriptor, params: Params): String =
    (msg.getFullName.lastIndexOf('.'), params.fallbackNamespace) match {
      case (i, _) if i > 1 => params.relocateNamespace(msg.getFullName.take(i))
      case (_, Some(b))    => b
      case _               => throw new IllegalArgumentException(s"Missing namespacle: ${msg.getFullName}")
    }
}

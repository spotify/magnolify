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

import java.io.{File, Reader}

import com.google.common.base.CaseFormat
import com.google.protobuf.Descriptors.FieldDescriptor.{Type => PType}
import org.apache.avro.Schema.{Type => AType}
object Yaml {
  case class Schema(
    avro: Option[Avro] = None,
    bigquery: Option[BigQuery] = None,
    protobuf: Option[Protobuf] = None
  )

  ////////////////////////////////////////////////////////////////////////////////

  /**
   * Generate code for Avro.
   * @param avsc `.avsc` schema files.
   * @param avro `.avro` data files.
   * @param `class` Java classes, e.g. Avro `SpecificRecord`.
   */
  case class Avro(
    avsc: List[String] = Nil,
    avro: List[String] = Nil,
    `class`: List[String] = Nil,
    params: Avro.Params = Avro.Params()
  )

  object Avro {
    case class Params(
      caseMapper: CaseMapper = CaseMapper.Identity,
      fallbackNamespace: Option[String] = None,
      relocateNamespace: RelocateNamespace = RelocateNamespace(),
      arrayType: String = "List",
      mapType: String = "Map",
      typeOverrides: Map[AType, String] = Map.empty,
      extraImports: List[String] = Nil
    ) {
      def get: AvroGen.Params = AvroGen.Params(
        caseMapper.get,
        fallbackNamespace,
        relocateNamespace.get,
        arrayType,
        mapType,
        typeOverrides,
        extraImports
      )
    }
  }

  ////////////////////////////////////////////////////////////////////////////////

  /**
   * Generate code for BigQuery.
   * @param fromTable generate from table specifications, e.g. `project_id:dataset_id.table_id`.
   * @param fromQuery generate from standard SQL queries.
   * @param fromStorage generate from storage API, including selected fields and row restrictions.
   */
  case class BigQuery(
    fromSchema: List[BigQuery.FromSchema] = Nil,
    fromTable: List[BigQuery.FromTable] = Nil,
    fromQuery: List[BigQuery.FromQuery] = Nil,
    fromStorage: List[BigQuery.FromStorage] = Nil,
    params: BigQuery.Params = BigQuery.Params()
  )

  object BigQuery {
    case class FromSchema(
      json: String,
      name: String,
      namespace: String,
      description: Option[String] = None
    )
    case class FromTable(
      table: String,
      name: String,
      namespace: String,
      description: Option[String] = None
    )
    case class FromQuery(
      query: String,
      name: String,
      namespace: String,
      description: Option[String] = None
    )
    case class FromStorage(
      table: String,
      name: String,
      namespace: String,
      description: Option[String] = None,
      selectedFields: List[String] = Nil,
      rowRestriction: Option[String] = None
    )

    case class Params(
      caseMapper: CaseMapper = CaseMapper.Identity,
      repeatedType: String = "List",
      typeOverrides: Map[String, String] = Map.empty,
      extraImports: List[String] = Nil
    ) {
      def get: TableRowGen.Params =
        TableRowGen.Params(caseMapper.get, repeatedType, typeOverrides, extraImports)
    }
  }

  ////////////////////////////////////////////////////////////////////////////////

  /**
   * Generate code for Protobuf.
   * @param proto `FileDescriptorSet` file produced by `protoc` flag `-o/--descriptor_set_out`.
   * @param `class` compiled Protobuf classes.
   */
  case class Protobuf(
    proto: List[String] = Nil,
    `class`: List[String] = Nil,
    params: Protobuf.Params = Protobuf.Params()
  )

  object Protobuf {
    case class Params(
      caseMapper: CaseMapper = CaseMapper.Identity,
      fallbackNamespace: Option[String] = None,
      relocateNamespace: RelocateNamespace = RelocateNamespace(),
      proto3Option: Boolean = false,
      repeatedType: String = "List",
      typeOverrides: Map[PType, String] = Map.empty,
      extraImports: List[String] = Nil
    ) {
      def get: ProtobufGen.Params = ProtobufGen.Params(
        caseMapper.get,
        fallbackNamespace,
        relocateNamespace.get,
        proto3Option,
        repeatedType,
        typeOverrides,
        extraImports
      )
    }
  }

  ////////////////////////////////////////////////////////////////////////////////

  sealed abstract class CaseMapper(val get: magnolify.shared.CaseMapper) {
    def this(t: (CaseFormat, CaseFormat)) =
      this(magnolify.shared.CaseMapper(t._1.converterTo(t._2).convert))
  }

  object CaseMapper {
    case object Identity extends CaseMapper(magnolify.shared.CaseMapper.identity)
    case object SnakeToCamel
        extends CaseMapper(CaseFormat.LOWER_UNDERSCORE -> CaseFormat.LOWER_CAMEL)
    case object CamelToSnake
        extends CaseMapper(CaseFormat.LOWER_CAMEL -> CaseFormat.LOWER_UNDERSCORE)
  }

  case class RelocateNamespace(prefix: String = "", suffix: String = "") {
    def get: String => String = ns => s"$prefix$ns$suffix"
  }

  ////////////////////////////////////////////////////////////////////////////////

  def parse(yaml: File): Schema = parse(Left(scala.io.Source.fromFile(yaml).reader()))
  def parse(yaml: String): Schema = parse(Right(yaml))

  private def parse(input: Either[Reader, String]): Schema = {
    import io.circe._
    import io.circe.generic.extras.auto._
    import io.circe.generic.extras.Configuration
    import io.circe.generic.extras.semiauto.deriveEnumerationCodec
    import io.circe.yaml.parser

    implicit val configuration: Configuration =
      Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames

    implicit val modeCodec: Codec[CaseMapper] = deriveEnumerationCodec[CaseMapper]

    implicit val avroKeyDecoder: KeyDecoder[AType] = KeyDecoder.decodeKeyString.map(AType.valueOf)
    implicit val avroKeyEncoder: KeyEncoder[AType] = KeyEncoder.encodeKeyString.contramap(_.name())

    implicit val protoKeyDecoder: KeyDecoder[PType] = KeyDecoder.decodeKeyString.map(PType.valueOf)
    implicit val protoKeyEncoder: KeyEncoder[PType] = KeyEncoder.encodeKeyString.contramap(_.name())

    val result = input match {
      case Left(r)  => parser.parse(r)
      case Right(s) => parser.parse(s)
    }
    result.flatMap(_.as[Schema]) match {
      case Left(err)    => throw new IllegalArgumentException(s"Failed to parse YAML: $err")
      case Right(value) => value
    }
  }
}

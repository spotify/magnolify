/*
 * Copyright 2021 Spotify AB
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

package magnolify.tools

sealed trait Schema

sealed trait Primitive extends Schema
sealed trait Composite extends Schema
final case class Record(
  name: Option[String],
//  namespace: Option[String], // TODO respect namespace
  doc: Option[String],
  fields: List[Record.Field]
) extends Composite

object Record {
  case class Field(
    name: String,
    doc: Option[String],
    schema: Schema
  )

}

case class Optional(schema: Schema) extends Composite
case class Repeated(schema: Schema) extends Composite
case class Mapped(keySchema: Schema, valueSchema: Schema) extends Composite

object Primitive {
  final case class Enum(
    name: Option[String],
//    namespace: Option[String],
    doc: Option[String],
    values: List[String]
  ) extends Primitive

  case object Null extends Primitive
  case object Boolean extends Primitive
  case object Char extends Primitive
  case object Byte extends Primitive
  case object Short extends Primitive
  case object Int extends Primitive
  case object Long extends Primitive
  case object Float extends Primitive
  case object Double extends Primitive
  case object String extends Primitive
  case object Bytes extends Primitive
  case object BigInt extends Primitive
  case object BigDecimal extends Primitive
  case object Instant extends Primitive
  case object LocalDateTime extends Primitive
  case object OffsetTime extends Primitive
  case object LocalTime extends Primitive
  case object LocalDate extends Primitive
  case object UUID extends Primitive
}

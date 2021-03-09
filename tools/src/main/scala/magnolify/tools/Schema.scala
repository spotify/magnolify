/*
 * Copyright 2021 Spotify AB.
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
package magnolify.tools

sealed trait Schema

sealed trait Primitive extends Schema

sealed trait Nested extends Schema

sealed trait Repetition

case object Required extends Repetition
case object Optional extends Repetition
case object Repeated extends Repetition

case class Record(
  name: Option[String],
  namespace: Option[String],
  doc: Option[String],
  fields: List[Field]
) extends Nested

case class Field(
  name: String,
  doc: Option[String],
  schema: Schema,
  repetition: Repetition
)

case class Enum(
  name: Option[String],
  namespace: Option[String],
  doc: Option[String],
  values: List[String]
) extends Nested

object Primitive {
  case object Unit extends Primitive
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

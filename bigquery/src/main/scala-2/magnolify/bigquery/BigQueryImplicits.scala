/*
 * Copyright 2022 Spotify AB
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

package magnolify.bigquery

import java.time._

trait BigQueryImplicits {
  implicit def tableRowType[T: TableRowField.Record]: TableRowType[T] = TableRowType[T]

  implicit val trfBool: TableRowField[Boolean] = TableRowField.trfBool
  implicit val trfLong: TableRowField[Long] = TableRowField.trfLong
  implicit val trfDouble: TableRowField[Double] = TableRowField.trfDouble
  implicit val trfString: TableRowField[String] = TableRowField.trfString
  implicit val trfNumeric: TableRowField[BigDecimal] = TableRowField.trfNumeric
  implicit val trfByteArray: TableRowField[Array[Byte]] = TableRowField.trfByteArray
  implicit val trfInstant: TableRowField[Instant] = TableRowField.trfInstant
  implicit val trfDate: TableRowField[LocalDate] = TableRowField.trfDate
  implicit val trfTime: TableRowField[LocalTime] = TableRowField.trfTime
  implicit val trfDateTime: TableRowField[LocalDateTime] = TableRowField.trfDateTime

  implicit def trfOption[T: TableRowField]: TableRowField[Option[T]] = TableRowField.trfOption
}

object BigQueryImplicits extends BigQueryImplicits

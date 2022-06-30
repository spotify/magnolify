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

import scala.collection.Factory

trait BigQueryImplicits:
  given tableRowType[T: TableRowField.Record]: TableRowType[T] = TableRowType[T]

  given trfBool: TableRowField[Boolean] = TableRowField.trfBool
  given trfLong: TableRowField[Long] = TableRowField.trfLong
  given trfDouble: TableRowField[Double] = TableRowField.trfDouble
  given trfString: TableRowField[String] = TableRowField.trfString
  given trfNumeric: TableRowField[BigDecimal] = TableRowField.trfNumeric
  given trfByteArray: TableRowField[Array[Byte]] = TableRowField.trfByteArray
  given trfInstant: TableRowField[Instant] = TableRowField.trfInstant
  given trfDate: TableRowField[LocalDate] = TableRowField.trfDate
  given trfTime: TableRowField[LocalTime] = TableRowField.trfTime
  given trfDateTime: TableRowField[LocalDateTime] = TableRowField.trfDateTime
  given trfOption[T: TableRowField]: TableRowField[Option[T]] = TableRowField.trfOption
  given trfIterable[T, C[_]](using
    TableRowField[T],
    C[T] => Iterable[T],
    Factory[T, C[T]]
  ): TableRowField[C[T]] =
    TableRowField.trfIterable

object BigQueryImplicits extends BigQueryImplicits
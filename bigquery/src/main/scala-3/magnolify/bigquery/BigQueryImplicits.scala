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
  given [T: TableRowField.Record]: TableRowType[T] = TableRowType[T]

  given TableRowField[Boolean] = TableRowField.trfBool
  given TableRowField[Long] = TableRowField.trfLong
  given TableRowField[Double] = TableRowField.trfDouble
  given TableRowField[String] = TableRowField.trfString
  given TableRowField[BigDecimal] = TableRowField.trfNumeric
  given TableRowField[Array[Byte]] = TableRowField.trfByteArray
  given TableRowField[Instant] = TableRowField.trfInstant
  given TableRowField[LocalDate] = TableRowField.trfDate
  given TableRowField[LocalTime] = TableRowField.trfTime
  given TableRowField[LocalDateTime] = TableRowField.trfDateTime
  given [T: TableRowField]: TableRowField[Option[T]] = TableRowField.trfOption
  given [T, C[_]](using TableRowField[T], C[T] => Iterable[T], Factory[T, C[T]]): TableRowField[C[T]] =
    TableRowField.trfIterable

object BigQueryImplicits extends BigQueryImplicits

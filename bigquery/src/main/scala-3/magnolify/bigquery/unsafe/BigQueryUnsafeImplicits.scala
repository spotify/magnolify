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

package magnolify.bigquery.unsafe

import magnolify.bigquery.TableRowField

trait BigQueryUnsafeImplicits:
  given TableRowField[Byte] = TableRowField.from[Long](_.toByte)(_.toLong)(TableRowField.trfLong)
  given TableRowField[Char] = TableRowField.from[Long](_.toChar)(_.toLong)(TableRowField.trfLong)
  given TableRowField[Short] = TableRowField.from[Long](_.toShort)(_.toLong)(TableRowField.trfLong)
  given TableRowField[Int] = TableRowField.from[Long](_.toInt)(_.toLong)(TableRowField.trfLong)
  given TableRowField[Float] =
    TableRowField.from[Double](_.toFloat)(_.toDouble)(TableRowField.trfDouble)

object BigQueryUnsafeImplicits extends BigQueryUnsafeImplicits

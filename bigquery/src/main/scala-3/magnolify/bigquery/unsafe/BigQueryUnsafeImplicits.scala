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
import magnolify.shared.{EnumType, UnsafeEnum}

trait BigQueryUnsafeImplicits:
  given trfByte: TableRowField[Byte] = TableRowField.trfByte
  given trfChar: TableRowField[Char] = TableRowField.trfChar
  given trfShort: TableRowField[Short] = TableRowField.trfShort
  given trfInt: TableRowField[Int] = TableRowField.trfInt
  given trfFloat: TableRowField[Float] = TableRowField.trfFloat
  given trfEnum[T](using EnumType[T]): TableRowField[T] = TableRowField.trfEnum
  given trfUnsafeEnum[T](using EnumType[T]): TableRowField[UnsafeEnum[T]] =
    TableRowField.trfUnsafeEnum

object BigQueryUnsafeImplicits extends BigQueryUnsafeImplicits

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

package magnolify.parquet

import magnolify.parquet.ParquetField.Primitive
import magnolify.shared._

import scala.annotation.nowarn

package object unsafe {
  implicit val pfChar: Primitive[Char] = ParquetField.from[Int](_.toChar)(_.toInt)

  @nowarn("msg=parameter value lp in method pfUnsafeEnum is never used")
  implicit def pfUnsafeEnum[T](implicit
    et: EnumType[T],
    lp: shapeless.LowPriority
  ): ParquetField[UnsafeEnum[T]] =
    ParquetField.from[String](UnsafeEnum.from(_))(UnsafeEnum.to(_))
}

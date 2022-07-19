/*
 * Copyright 2020 Spotify AB
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

package magnolify.datastore.unsafe

import magnolify.datastore._
import magnolify.shared._

trait EntityUnsafeImplicits {
  implicit val efByte: EntityField[Byte] = EntityField.efByte
  implicit val efChar: EntityField[Char] = EntityField.efChar
  implicit val efShort: EntityField[Short] = EntityField.efShort
  implicit val efInt: EntityField[Int] = EntityField.efInt
  implicit def efFloat(implicit kf: KeyField[Double]): EntityField[Float] = EntityField.efFloat
  implicit def efEnum[T](implicit et: EnumType[T]): EntityField[T] = EntityField.efEnum
  implicit def efUnsafeEnum[T](implicit et: EnumType[T]): EntityField[UnsafeEnum[T]] =
    EntityField.efUnsafeEnum
}

object EntityUnsafeImplicits extends EntityUnsafeImplicits

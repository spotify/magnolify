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

package magnolify.datastore

import magnolify.shared._

package object unsafe {
  implicit val efByte: EntityField[Byte] = EntityField.from[Long](_.toByte)(_.toLong)
  implicit val efChar: EntityField[Char] = EntityField.from[Long](_.toChar)(_.toLong)
  implicit val efShort: EntityField[Short] = EntityField.from[Long](_.toShort)(_.toLong)
  implicit val efInt: EntityField[Int] = EntityField.from[Long](_.toInt)(_.toLong)
  implicit val efFloat: EntityField[Float] = EntityField.from[Double](_.toFloat)(_.toDouble)

  implicit def efEnum[T](implicit et: EnumType[T]): EntityField[T] =
    EntityField.from[String](et.from)(et.to)

  implicit def efUnsafeEnum[T: EnumType]: EntityField[UnsafeEnum[T]] =
    EntityField.from[String](UnsafeEnum.from[T])(UnsafeEnum.to[T])
}

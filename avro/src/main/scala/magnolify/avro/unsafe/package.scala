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

package magnolify.avro

import magnolify.shared._

package object unsafe extends UnsafeAvroFieldInstance0

trait UnsafeAvroFieldInstance0 {
  implicit val afByte: AvroField[Byte] = AvroField.from[Int](_.toByte)(_.toInt)
  implicit val afChar: AvroField[Char] = AvroField.from[Int](_.toChar)(_.toInt)
  implicit val afShort: AvroField[Short] = AvroField.from[Int](_.toShort)(_.toInt)

  implicit def afUnsafeEnum[T: EnumType]: AvroField[UnsafeEnum[T]] =
    AvroField.from[String](UnsafeEnum.from(_))(UnsafeEnum.to(_))
}

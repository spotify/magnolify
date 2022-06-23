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

package magnolify.bigtable

import java.util.UUID

import com.google.protobuf.ByteString
import magnolify.shared.{EnumType, UnsafeEnum}
import magnolify.bigtable.BigtableField

import scala.collection.Factory

trait BigtableImplicits extends LowPriorityImplicits:
  given [T: BigtableField.Record]: BigtableType[T] = BigtableType[T]

  given BigtableField.Primitive[Byte] = BigtableField.btfByte
  given BigtableField.Primitive[Char] = BigtableField.btChar
  given BigtableField.Primitive[Short] = BigtableField.btfShort
  given BigtableField.Primitive[Int] = BigtableField.btfInt
  given BigtableField.Primitive[Long] = BigtableField.btfLong
  given BigtableField.Primitive[Float] = BigtableField.btfFloat
  given BigtableField.Primitive[Double] = BigtableField.btfDouble
  given BigtableField.Primitive[Boolean] = BigtableField.btfBoolean
  given BigtableField.Primitive[UUID] = BigtableField.btfUUID
  given BigtableField.Primitive[ByteString] = BigtableField.btfByteString
  given BigtableField.Primitive[Array[Byte]] = BigtableField.btfByteArray
  given BigtableField.Primitive[String] = BigtableField.btfString
  given [T: EnumType]: BigtableField.Primitive[UnsafeEnum[T]] = BigtableField.btfUnsafeEnum

  given BigtableField.Primitive[BigInt] = BigtableField.btfBigInt
  given BigtableField.Primitive[BigDecimal] = BigtableField.btfBigDecimal
  given [T: BigtableField]: BigtableField[Option[T]] = BigtableField.btfOption[T]
  given [T, C[_]](using
    BigtableField.Primitive[T],
    C[T] => Iterable[T],
    Factory[T, C[T]]
  ): BigtableField.Primitive[C[T]] =
    BigtableField.btfIterable

trait LowPriorityImplicits:
  given [T: EnumType]: BigtableField.Primitive[T] = BigtableField.btfEnum

object BigtableImplicits extends BigtableImplicits

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
  given bigtableType[T: BigtableField.Record]: BigtableType[T] = BigtableType[T]

  given btfByte: BigtableField.Primitive[Byte] = BigtableField.btfByte
  given btChar: BigtableField.Primitive[Char] = BigtableField.btChar
  given btfShort: BigtableField.Primitive[Short] = BigtableField.btfShort
  given btfInt: BigtableField.Primitive[Int] = BigtableField.btfInt
  given btfLong: BigtableField.Primitive[Long] = BigtableField.btfLong
  given btfFloat: BigtableField.Primitive[Float] = BigtableField.btfFloat
  given btfDouble: BigtableField.Primitive[Double] = BigtableField.btfDouble
  given btfBoolean: BigtableField.Primitive[Boolean] = BigtableField.btfBoolean
  given btfUUID: BigtableField.Primitive[UUID] = BigtableField.btfUUID
  given btfByteString: BigtableField.Primitive[ByteString] = BigtableField.btfByteString
  given btfByteArray: BigtableField.Primitive[Array[Byte]] = BigtableField.btfByteArray
  given btfString: BigtableField.Primitive[String] = BigtableField.btfString
  given btfUnsafeEnum[T: EnumType]: BigtableField.Primitive[UnsafeEnum[T]] =
    BigtableField.btfUnsafeEnum

  given btfBigInt: BigtableField.Primitive[BigInt] = BigtableField.btfBigInt
  given btfBigDecimal: BigtableField.Primitive[BigDecimal] = BigtableField.btfBigDecimal
  given btfOption[T: BigtableField]: BigtableField[Option[T]] = BigtableField.btfOption[T]
  given btfIterable[T, C[_]](using
    BigtableField.Primitive[T],
    C[T] => Iterable[T],
    Factory[T, C[T]]
  ): BigtableField.Primitive[C[T]] =
    BigtableField.btfIterable

trait LowPriorityImplicits:
  given btfEnum[T: EnumType]: BigtableField.Primitive[T] = BigtableField.btfEnum

object BigtableImplicits extends BigtableImplicits

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

import com.google.protobuf.ByteString
import magnolify.bigtable.BigtableField

import java.util.UUID

trait BigtableImplicits:
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

//  implicit def btfEnum[T](implicit et: EnumType[T], lp: shapeless.LowPriority): Primitive[T] =
//  implicit def btfUnsafeEnum[T](implicit
//                                et: EnumType[T],
//                                lp: shapeless.LowPriority
//                               ): Primitive[UnsafeEnum[T]] =

  given BigtableField.Primitive[BigInt] = BigtableField.btfBigInt
  given BigtableField.Primitive[BigDecimal] = BigtableField.btfBigDecimal
  given [T: BigtableField]: BigtableField[Option[T]] = BigtableField.btfOption[T]
//  implicit def btfIterable[T, C[T]](implicit
//                                    btf: Primitive[T],
//                                    ti: C[T] => Iterable[T],
//                                    fc: FactoryCompat[T, C[T]]
//                                   ): Primitive[C[T]] =

object BigtableImplicits extends BigtableImplicits

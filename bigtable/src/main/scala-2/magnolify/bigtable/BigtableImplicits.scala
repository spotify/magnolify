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

import java.util.UUID

import magnolify.shared.{EnumType, UnsafeEnum}

import scala.collection.compat._

trait BigtableImplicits extends LowPriorityImplicits {
  implicit def bigtableType[T: BigtableField.Record]: BigtableType[T] = BigtableType[T]

  implicit val btfByte: BigtableField.Primitive[Byte] = BigtableField.btfByte
  implicit val btChar: BigtableField.Primitive[Char] = BigtableField.btChar
  implicit val btfShort: BigtableField.Primitive[Short] = BigtableField.btfShort
  implicit val btfInt: BigtableField.Primitive[Int] = BigtableField.btfInt
  implicit val btfLong: BigtableField.Primitive[Long] = BigtableField.btfLong
  implicit val btfFloat: BigtableField.Primitive[Float] = BigtableField.btfFloat
  implicit val btfDouble: BigtableField.Primitive[Double] = BigtableField.btfDouble
  implicit val btfBoolean: BigtableField.Primitive[Boolean] = BigtableField.btfBoolean
  implicit val btfUUID: BigtableField.Primitive[UUID] = BigtableField.btfUUID
  implicit val btfByteString: BigtableField.Primitive[ByteString] = BigtableField.btfByteString
  implicit val btfByteArray: BigtableField.Primitive[Array[Byte]] = BigtableField.btfByteArray
  implicit val btfString: BigtableField.Primitive[String] = BigtableField.btfString
  implicit def btfUnsafeEnum[T: EnumType]: BigtableField.Primitive[UnsafeEnum[T]] =
    BigtableField.btfUnsafeEnum
  implicit val btfBigInt: BigtableField.Primitive[BigInt] = BigtableField.btfBigInt
  implicit val btfBigDecimal: BigtableField.Primitive[BigDecimal] = BigtableField.btfBigDecimal
  implicit def btfOption[T: BigtableField]: BigtableField[Option[T]] = BigtableField.btfOption
  implicit def btfIterable[T, C[T]](implicit
    btf: BigtableField.Primitive[T],
    ti: C[T] => Iterable[T],
    fc: Factory[T, C[T]]
  ): BigtableField[C[T]] = BigtableField.btfIterable
}

trait LowPriorityImplicits {
  implicit def btfEnum[T: EnumType]: BigtableField.Primitive[T] = BigtableField.btfEnum
}

object BigtableImplicits extends BigtableImplicits

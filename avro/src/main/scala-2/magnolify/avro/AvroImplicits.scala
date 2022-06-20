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

package magnolify.avro

import java.util.UUID
import java.time.LocalDate
import magnolify.shared.EnumType
import scala.collection.compat._

trait AvroImplicits {

  implicit def avroType[T: AvroField.Record]: AvroType[T] = AvroType[T]

  implicit val afBoolean: AvroField[Boolean] = AvroField.afBoolean
  implicit val afInt: AvroField[Int] = AvroField.afInt
  implicit val afLong: AvroField[Long] = AvroField.afLong
  implicit val afFloat: AvroField[Float] = AvroField.afFloat
  implicit val afDouble: AvroField[Double] = AvroField.afDouble
  implicit val afString: AvroField[String] = AvroField.afString
  implicit val afUnit: AvroField[Unit] = AvroField.afUnit
  implicit val afBytes: AvroField[Array[Byte]] = AvroField.afBytes
  implicit def afEnum[T](implicit et: EnumType[T]): AvroField[T] = AvroField.afEnum
  implicit def afOption[T](implicit f: AvroField[T]): AvroField[Option[T]] = AvroField.afOption
  implicit def afIterable[T, C[_]](implicit
    f: AvroField[T],
    ti: C[T] => Iterable[T],
    fc: Factory[T, C[T]]
  ): AvroField[C[T]] = AvroField.afIterable
  implicit def afMap[T](implicit f: AvroField[T]): AvroField[Map[String, T]] = AvroField.afMap

  implicit val afUuid: AvroField[UUID] = AvroField.afUuid
  implicit val afDate: AvroField[LocalDate] = AvroField.afDate
}

object AvroImplicits extends AvroImplicits

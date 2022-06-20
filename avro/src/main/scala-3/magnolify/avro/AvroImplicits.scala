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

import magnolify.avro.AvroField.{aux, aux2, id}
import magnolify.shared.{CaseMapper, EnumType}
import org.apache.avro.{JsonProperties, LogicalType, LogicalTypes, Schema}
import org.apache.avro.generic.{GenericArray, GenericData}

import java.nio.ByteBuffer
import java.util.UUID
import java.time.LocalDate
import scala.collection.Factory
import scala.jdk.CollectionConverters.*

trait AvroImplicits:

  given [T](using AvroField.Record[T]): AvroType[T] = AvroType[T]

  given AvroField[Boolean] = AvroField.afBoolean
  given AvroField[Int] = AvroField.afInt
  given AvroField[Long] = AvroField.afLong
  given AvroField[Float] = AvroField.afFloat
  given AvroField[Double] = AvroField.afDouble
  given AvroField[String] = AvroField.afString
  given AvroField[Unit] = AvroField.afUnit
  given AvroField[Array[Byte]] = AvroField.afBytes
  given [T: EnumType]: AvroField[T] = AvroField.afEnum
  given [T: AvroField]: AvroField[Option[T]] = AvroField.afOption
  given [T, C[_]](using AvroField[T], C[T] => Iterable[T], Factory[T, C[T]]): AvroField[C[T]] =
    AvroField.afIterable
  given [T: AvroField]: AvroField[Map[String, T]] = AvroField.afMap

  given AvroField[UUID] = AvroField.afUuid
  given AvroField[LocalDate] = AvroField.afDate

object AvroImplicits extends AvroImplicits

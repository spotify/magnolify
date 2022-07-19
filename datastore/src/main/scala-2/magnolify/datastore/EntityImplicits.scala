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

package magnolify.datastore

import magnolify.datastore._
import com.google.protobuf.ByteString

import scala.collection.compat._

import java.time.Instant

trait EntityImplicits {
  implicit def entityType[T](implicit ef: EntityField.Record[T]): EntityType[T] = EntityType[T]

  implicit val longKeyField: KeyField[Long] = KeyField.longKeyField
  implicit val stringKeyField: KeyField[String] = KeyField.stringKeyField
  implicit def notSupportedKeyField[T]: KeyField[T] = new KeyField.NotSupported[T]

  implicit val efLong: EntityField[Long] = EntityField.efLong
  implicit val efString: EntityField[String] = EntityField.efString
  implicit def efBool(implicit kf: KeyField[Boolean]): EntityField[Boolean] = EntityField.efBool
  implicit def efDouble(implicit kf: KeyField[Double]): EntityField[Double] = EntityField.efDouble
  implicit def efUnit(implicit kf: KeyField[Unit]): EntityField[Unit] = EntityField.efUnit
  implicit def efByteString(implicit kf: KeyField[ByteString]): EntityField[ByteString] =
    EntityField.efByteString
  implicit def efByteArray(implicit kf: KeyField[Array[Byte]]): EntityField[Array[Byte]] =
    EntityField.efByteArray
  implicit val efTimestamp: EntityField[Instant] = EntityField.efTimestamp
  implicit def efOption[T](implicit kf: EntityField[T]): EntityField[Option[T]] =
    EntityField.efOption
  implicit def efIterable[T, C[_]](implicit
    ef: EntityField[T],
    kf: KeyField[C[T]],
    ti: C[T] => Iterable[T],
    fc: Factory[T, C[T]]
  ): EntityField[C[T]] = EntityField.efIterable
}

object EntityImplicits extends EntityImplicits

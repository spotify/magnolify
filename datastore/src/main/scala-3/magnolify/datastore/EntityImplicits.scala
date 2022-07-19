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

import com.google.protobuf.ByteString
import java.time.Instant
import scala.collection.compat.*

trait EntityImplicits:
  given entityType[T](using EntityField.Record[T]): EntityType[T] = EntityType[T]

  given longKeyField: KeyField[Long] = KeyField.longKeyField
  given stringKeyField: KeyField[String] = KeyField.stringKeyField
  given notSupportedKeyField[T]: KeyField[T] = new KeyField.NotSupported[T]

  given efLong: EntityField[Long] = EntityField.efLong
  given efString: EntityField[String] = EntityField.efString
  given efBool(using KeyField[Boolean]): EntityField[Boolean] = EntityField.efBool
  given efDouble(using KeyField[Double]): EntityField[Double] = EntityField.efDouble
  given efUnit(using KeyField[Unit]): EntityField[Unit] = EntityField.efUnit
  given efByteString(using KeyField[ByteString]): EntityField[ByteString] = EntityField.efByteString
  given efByteArray(using KeyField[Array[Byte]]): EntityField[Array[Byte]] = EntityField.efByteArray
  given efTimestamp: EntityField[Instant] = EntityField.efTimestamp
  given efOption[T](using EntityField[T]): EntityField[Option[T]] = EntityField.efOption
  given efIterable[T, C[_]](using
    EntityField[T],
    KeyField[C[T]],
    C[T] => Iterable[T],
    Factory[T, C[T]]
  ): EntityField[C[T]] = EntityField.efIterable

object EntityImplicits extends EntityImplicits

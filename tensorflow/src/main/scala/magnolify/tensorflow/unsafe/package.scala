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

package magnolify.tensorflow

import com.google.protobuf.ByteString
import magnolify.shared._

package object unsafe {
  implicit val efByte = ExampleField.from[Long](_.toByte)(_.toLong)
  implicit val efChar = ExampleField.from[Long](_.toChar)(_.toLong)
  implicit val efShort = ExampleField.from[Long](_.toShort)(_.toLong)
  implicit val efInt = ExampleField.from[Long](_.toInt)(_.toLong)
  implicit val efDouble = ExampleField.from[Float](_.toDouble)(_.toFloat)
  implicit val efBool = ExampleField.from[Long](_ == 1)(x => if (x) 1 else 0)
  implicit val efString = ExampleField.from[ByteString](_.toStringUtf8)(ByteString.copyFromUtf8)

  implicit def efEnum[T](implicit
    et: EnumType[T],
    lp: shapeless.LowPriority
  ): ExampleField.Primitive[T] =
    ExampleField.from[ByteString](bs => et.from(bs.toStringUtf8))(v =>
      ByteString.copyFromUtf8(v.toString)
    )

  implicit def efUnsafeEnum[T](implicit
    et: EnumType[T],
    lp: shapeless.LowPriority
  ): ExampleField.Primitive[UnsafeEnum[T]] =
    ExampleField.from[ByteString](bs => UnsafeEnum.from(bs.toStringUtf8))(v =>
      ByteString.copyFromUtf8(UnsafeEnum.to(v))
    )
}

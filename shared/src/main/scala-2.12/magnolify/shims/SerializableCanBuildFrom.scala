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

package magnolify.shims

import scala.collection.compat.immutable.LazyList
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.reflect.ClassTag

// For scala 2.12, scala.collection.compat.Factor is an alias for scala.collection.generic.CanBuildFrom
// CanBuildFrom is not serializable
// We need to create our own serializable factory
trait SerializableCanBuildFrom[-From, -Elem, +To]
    extends CanBuildFrom[From, Elem, To]
    with Serializable

object SerializableCanBuildFrom {
  private[shims] def apply[A, C](b: => mutable.Builder[A, C]): SerializableCanBuildFrom[Any, A, C] =
    new SerializableCanBuildFrom[Any, A, C] {
      override def apply(from: Any): mutable.Builder[A, C] = apply()
      override def apply(): mutable.Builder[A, C] = b
    }
}

trait SerializableCanBuildFromInstances extends SerializableCanBuildFromLowPrio {
  implicit def arrayCBF[A: ClassTag]: SerializableCanBuildFrom[Any, A, Array[A]] =
    SerializableCanBuildFrom(Array.newBuilder[A])
}

trait SerializableCanBuildFromLowPrio extends SerializableCanBuildFromLowPrio2 {
  implicit def listCBF[A: ClassTag]: SerializableCanBuildFrom[Any, A, List[A]] =
    SerializableCanBuildFrom(List.newBuilder[A])
}

trait SerializableCanBuildFromLowPrio2 extends SerializableCanBuildFromLowPrio3 {
  implicit def vectorCBF[A: ClassTag]: SerializableCanBuildFrom[Any, A, Vector[A]] =
    SerializableCanBuildFrom(Vector.newBuilder[A])
}

trait SerializableCanBuildFromLowPrio3 extends SerializableCanBuildFromLowPrio4 {
  implicit def streamCBF[A: ClassTag]: SerializableCanBuildFrom[Any, A, Stream[A]] =
    SerializableCanBuildFrom(Stream.newBuilder[A])
}

trait SerializableCanBuildFromLowPrio4 {
  implicit def lazyListCBF[A: ClassTag]: SerializableCanBuildFrom[Any, A, LazyList[A]] =
    SerializableCanBuildFrom(LazyList.newBuilder[A])
}

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

package magnolify.neo4j

import magnolify.shared._
import scala.annotation.nowarn

package object unsafe {

  @nowarn("msg=parameter value lp in method vfEnum is never used")
  implicit def vfEnum[T](implicit
    et: EnumType[T],
    lp: shapeless.LowPriority
  ): ValueField[T] =
    ValueField.from[String](et.from)(_.toString)

  @nowarn("msg=parameter value lp in method vfUnsafeEnum is never used")
  implicit def vfUnsafeEnum[T](implicit
    et: EnumType[T],
    lp: shapeless.LowPriority
  ): ValueField[UnsafeEnum[T]] =
    ValueField.from[String](UnsafeEnum.from(_))(UnsafeEnum.to(_))
}

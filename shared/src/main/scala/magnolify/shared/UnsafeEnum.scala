/*
 * Copyright 2021 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package magnolify.shared

sealed trait UnsafeEnum[+T]

object UnsafeEnum {
  case class Known[T](value: T) extends UnsafeEnum[T]
  case class Unknown(value: String) extends UnsafeEnum[Nothing] {
    require(value != null && value.nonEmpty, s"Invalid enum value: $value")
  }

  def apply[T](value: T): Known[T] = Known(value)

  def from[T](value: String)(implicit et: EnumType[T]): UnsafeEnum[T] =
    if (et.valueSet.contains(value)) Known(et.from(value)) else Unknown(value)

  def to[T](value: UnsafeEnum[T])(implicit et: EnumType[T]): String =
    value match {
      case Known(value)   => et.to(value)
      case Unknown(value) => value
    }
  def to[T](value: Known[T])(implicit et: EnumType[T]): String = et.to(value.value)
  def to(value: Unknown): String = value.value
}

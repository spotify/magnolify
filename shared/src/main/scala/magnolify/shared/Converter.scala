/*
 * Copyright 2019 Spotify AB
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

package magnolify.shared

trait Converter[T, Reader, Writer] extends Serializable {
  def from(v: Reader): T
  def to(v: T): Writer
}

// Represent a value from an external source.
sealed trait Value[+T] {
  def get: T = this match {
    case Value.Some(v)    => v
    case Value.Default(v) => v
    case Value.None       => throw new NoSuchElementException
  }

  def isSome: Boolean = this.isInstanceOf[Value.Some[_]]
  def isEmpty: Boolean = this eq Value.None

  def getOrElse[U](fallback: Option[U])(implicit ev: T <:< U): U = (this, fallback) match {
    case (Value.Some(x), _)          => x
    case (Value.Default(_), Some(x)) => x
    case (Value.Default(x), None)    => x
    case (Value.None, Some(x))       => x
    case _                           => throw new NoSuchElementException
  }

  def toOption: Value[Option[T]] = this match {
    case Value.Some(v)    => Value.Some(Some(v))
    case Value.Default(v) => Value.Default(Some(v))
    case Value.None       => Value.Default(None)
  }
}

object Value {
  // Value from the external source, e.g. Avro, BigQuery
  case class Some[T](value: T) extends Value[T]

  // Value from the case class default
  case class Default[T](value: T) extends Value[T]

  // Value missing from both the external source and the case class default
  case object None extends Value[Nothing]
}

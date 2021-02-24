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
package magnolify.parquet

import org.apache.parquet.io.api.{Binary, Converter, GroupConverter, PrimitiveConverter}
import org.apache.parquet.schema.Type.Repetition

import scala.collection.mutable

sealed trait TypeConverter[T] extends Converter {
  def get: T
}

private object TypeConverter {
  trait Buffered[T] extends TypeConverter[T] {
    private val buffer: mutable.Buffer[T] = mutable.Buffer.empty
    private var _repetition: Repetition = Repetition.REQUIRED
    def repetition: Repetition = _repetition

    override def get: T = get { b =>
      require(b.size == 1, "Required field size != 1: " + buffer.size)
      b.head
    }

    def get[R](f: mutable.Buffer[T] => R): R = {
      val r = f(buffer)
      buffer.clear()
      r
    }

    def addValue(value: T): Unit = {
      if (repetition != Repetition.REPEATED) buffer.clear()
      buffer += value
    }

    def withRepetition(repetition: Repetition): Buffered[T] = {
      _repetition = repetition
      this
    }
  }

  abstract class Delegate[V, U](val inner: Buffered[V]) extends TypeConverter[U] {
    override def isPrimitive: Boolean = inner.isPrimitive
    override def asPrimitiveConverter(): PrimitiveConverter = {
      require(isPrimitive)
      inner.asPrimitiveConverter()
    }
    override def asGroupConverter(): GroupConverter = {
      require(!isPrimitive)
      inner.asGroupConverter()
    }
  }

  abstract class Primitive[T] extends PrimitiveConverter with Buffered[T] { self =>
    def map[U](f: T => U): TypeConverter[U] = new Primitive[U] {
      override def repetition: Repetition = self.repetition
      override def get: U = f(self.get)
      override def get[R](g: mutable.Buffer[U] => R): R = self.get(b => g(b.map(f)))

      override def withRepetition(repetition: Repetition): Buffered[U] = {
        self.withRepetition(repetition)
        this
      }

      // We don't know which method was overridden, delegate all
      override def addBinary(value: Binary): Unit = self.addBinary(value)
      override def addBoolean(value: Boolean): Unit = self.addBoolean(value)
      override def addDouble(value: Double): Unit = self.addDouble(value)
      override def addFloat(value: Float): Unit = self.addFloat(value)
      override def addInt(value: Int): Unit = self.addInt(value)
      override def addLong(value: Long): Unit = self.addLong(value)
    }
  }

  def newBoolean: Primitive[Boolean] = new Primitive[Boolean] {
    override def addBoolean(value: Boolean): Unit = addValue(value)
  }
  def newInt: Primitive[Int] = new Primitive[Int] {
    override def addInt(value: Int): Unit = addValue(value)
  }
  def newLong: Primitive[Long] = new Primitive[Long] {
    override def addLong(value: Long): Unit = addValue(value)
  }
  def newFloat: Primitive[Float] = new Primitive[Float] {
    override def addFloat(value: Float): Unit = addValue(value)
  }
  def newDouble: Primitive[Double] = new Primitive[Double] {
    override def addDouble(value: Double): Unit = addValue(value)
  }
  def newByteArray: Primitive[Array[Byte]] = new Primitive[Array[Byte]] {
    override def addBinary(value: Binary): Unit = addValue(value.getBytes)
  }
  def newString: Primitive[String] = new Primitive[String] {
    override def addBinary(value: Binary): Unit = addValue(value.toStringUsingUTF8)
  }
}

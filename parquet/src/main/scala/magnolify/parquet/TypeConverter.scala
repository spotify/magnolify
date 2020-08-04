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

import scala.collection.mutable

sealed trait TypeConverter[T] extends Converter { self =>
  def get: T
}

private object TypeConverter {
  trait Buffered[T] extends TypeConverter[T] {
    var isRepeated: Boolean = false
    val buffer: mutable.Buffer[T] = mutable.Buffer.empty
    override def get: T = {
      require(buffer.size == 1, "Required field size != 1: " + buffer.size)
      val v = buffer.head
      buffer.clear()
      v
    }
  }

  abstract class Primitive[T] extends PrimitiveConverter with Buffered[T] { self =>
    protected def addValue(value: T): Unit = {
      if (!isRepeated) buffer.clear()
      buffer += value
    }
    def map[U](f: T => U): TypeConverter[U] = new TypeConverter.Primitive[U] {
      override def get: U = f(self.get)
      override def isPrimitive: Boolean = self.isPrimitive

      // We don't know which method was overridden, delegate all
      override def addBinary(value: Binary): Unit = self.addBinary(value)
      override def addBoolean(value: Boolean): Unit = self.addBoolean(value)
      override def addDouble(value: Double): Unit = self.addDouble(value)
      override def addFloat(value: Float): Unit = self.addFloat(value)
      override def addInt(value: Int): Unit = self.addInt(value)
      override def addLong(value: Long): Unit = self.addLong(value)
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

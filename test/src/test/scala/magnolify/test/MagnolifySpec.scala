/*
 * Copyright 2019 Spotify AB.
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
package magnolify.test

import java.io._

import org.scalacheck._

import scala.reflect._

abstract class MagnolifySpec(name: String) extends Properties(name) {
  def className[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName

  private def serializeToByteArray(value: Serializable): Array[Byte] = {
    val buffer = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(buffer)
    oos.writeObject(value)
    buffer.toByteArray
  }

  private def deserializeFromByteArray(encodedValue: Array[Byte]): AnyRef = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(encodedValue))
    ois.readObject()
  }

  def ensureSerializable[T <: Serializable](value: T): T =
    deserializeFromByteArray(serializeToByteArray(value)).asInstanceOf[T]
}

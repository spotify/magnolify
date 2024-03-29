/*
 * Copyright 2021 Spotify AB
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

package magnolify.parquet

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.google.common.io.BaseEncoding

private object SerializationUtils {
  def toBase64[T <: Serializable](obj: T): String = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(obj)
    oos.close()
    BaseEncoding.base64().encode(baos.toByteArray)
  }

  def fromBase64[T](b64: String): T = {
    val bytes = BaseEncoding.base64().decode(b64)
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    ois.readObject().asInstanceOf[T]
  }
}

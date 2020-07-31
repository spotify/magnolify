/*
 * Copyright 2020 Spotify AB.
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
package magnolify.codegen

import java.io.{File, FileInputStream}

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.{Descriptor, FileDescriptor}
import magnolify.shims.JavaConverters._

object ProtobufClient {
  def fromProto(proto: String): List[Descriptor] = {
    val fis = new FileInputStream(new File(proto))
    val descriptors = FileDescriptorSet
      .parseFrom(fis)
      .getFileList
      .asScala
      .flatMap { f =>
        FileDescriptor.buildFrom(f, Array.empty, false).getMessageTypes.asScala
      }
      .toList
    fis.close()
    descriptors
  }

  def fromClass(cls: String): Descriptor =
    getClass.getClassLoader
      .loadClass(cls)
      .getMethod("getDescriptor")
      .invoke(null)
      .asInstanceOf[Descriptor]
}

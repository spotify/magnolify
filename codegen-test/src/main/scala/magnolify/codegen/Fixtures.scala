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

import java.io.{FileOutputStream, PrintWriter}
import java.nio.file.{Path, Paths}

import magnolify.codegen.avro.AvroFixtures
import magnolify.codegen.bigquery.TableRowFixtures
import magnolify.codegen.protobuf.ProtobufFixtures

import scala.reflect.ClassTag

object Fixtures {
  case class A(i: Long)
  def main(args: Array[String]): Unit =
    gen(Paths.get(args(0)), Paths.get(args(1)))

  def gen(srcDir: Path, rsrcDir: Path): Unit = {
    new AvroFixtures(srcDir, rsrcDir).gen()
    new ProtobufFixtures(srcDir, rsrcDir).gen()
    new TableRowFixtures(srcDir, rsrcDir).gen()
  }
}

abstract class BaseFixtures {
  def name[T](implicit ct: ClassTag[T]): String = ct.runtimeClass.getSimpleName
  val namespace: String = getClass.getPackage.getName

  def saveSchema(
    rsrcDir: Path,
    name: String,
    namespace: String,
    schemaString: String,
    suffix: String
  ): Unit = {
    val schemaDir = namespace.split('.').foldLeft(rsrcDir)(_.resolve(_))
    val schemaFile = schemaDir.resolve(name + suffix)
    schemaDir.toFile.mkdirs()
    val fos = new FileOutputStream(schemaFile.toFile)
    val writer = new PrintWriter(fos)
    writer.write(schemaString)
    writer.close()
    fos.close()
  }
}

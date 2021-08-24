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
package magnolify.parquet.test

import magnolify.avro._
import magnolify.parquet._
import magnolify.scalacheck.auto._
import magnolify.test.Simple._
import munit._
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.avro.AvroParquetWriter
import org.scalacheck._

class AvroCompatSuite extends FunSuite {
  private val seed: rng.Seed = rng.Seed(0)
  private val prms: Gen.Parameters = Gen.Parameters.default
  private val record = implicitly[Arbitrary[Collections]].arbitrary(prms, seed).get

  test("Fail read Avro Parquet without AvroCompat") {
    val at = AvroType[Collections]
    val out = new TestOutputFile
    val writer = AvroParquetWriter.builder[GenericRecord](out).withSchema(at.schema).build()
    writer.write(at(record))
    writer.close()

    val pt = ParquetType[Collections]
    val in = new TestInputFile(out.getBytes)
    val reader = pt.readBuilder(in).build()
    val msg = "requirement failed: " +
      "Parquet file was written from Avro records, " +
      "`import magnolify.parquet.ParquetArray.AvroCompat._` to read correctly"
    interceptMessage[IllegalArgumentException](msg)(reader.read())
  }

  test("Fail read vanilla Parquet with AvroCompat") {
    val pt1 = ParquetType[Collections]

    val out = new TestOutputFile
    val writer = pt1.writeBuilder(out).build()
    writer.write(record)
    writer.close()

    val pt2 = {
      import magnolify.parquet.ParquetArray.AvroCompat._
      ParquetType[Collections]
    }
    val in = new TestInputFile(out.getBytes)
    val reader = pt2.readBuilder(in).build()
    val msg = "requirement failed: " +
      "Parquet file was not written from Avro records, " +
      "remove `import magnolify.parquet.ParquetArray.AvroCompat._` to read correctly"
    interceptMessage[IllegalArgumentException](msg)(reader.read())
  }
}

/*
 * Copyright 2020 Spotify AB
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

package magnolify.avro.test

import java.io.File
import java.time._

import magnolify.avro._
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}

// Test BigQuery data types, to be used with scripts/bigquery-test.sh
object TestData
    extends magnolify.avro.AutoDerivation
    with magnolify.avro.AvroImplicits
    with magnolify.avro.logical.AvroBigQueryImplicits {

  def main(args: Array[String]): Unit = {
    val outDir = new File(args(0))
    outDir.mkdirs()

    def write(name: String, r: GenericRecord): Unit = {
      val datumWriter = new GenericDatumWriter[GenericRecord]()
      val writer = new DataFileWriter[GenericRecord](datumWriter)
      writer.create(r.getSchema, outDir.toPath.resolve(name).toFile)
      writer.append(r)
      writer.close()
    }

    val t = AvroType[Record]

    // 2020-08-01 01:23:45
    val ts = Instant.ofEpochSecond(1596245025)
    val zdt = ts.atZone(ZoneOffset.UTC)
    val r = Record(BigDecimal(123.456), ts, zdt.toLocalDate, zdt.toLocalTime, zdt.toLocalDateTime)
    write("avro1.avro", t(r))
    // precision > 38
    // write("avro2.avro", t(r.copy(BigDecimal("1234567890123456789012345678901234567890"))))
    // scale > 9
    // write("avro3.avro", t(r.copy(BigDecimal("3.14159265358979323846"))))
  }

  case class Record(bd: BigDecimal, ts: Instant, d: LocalDate, t: LocalTime, dt: LocalDateTime)
}

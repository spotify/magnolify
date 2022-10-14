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

package magnolify.bigquery

import java.io.File
import java.time._

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import magnolify.bigquery._

// Test BigQuery data types, to be used with scripts/bigquery-test.sh
object TestData {
  def main(args: Array[String]): Unit = {
    val outDir = new File(args(0))
    outDir.mkdirs()

    val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    def write(name: String, v: AnyRef): Unit =
      mapper.writeValue(outDir.toPath.resolve(name).toFile, v)

    val t = TableRowType[Record]
    write("schema.json", t.schema.getFields)

    // 2020-08-01 01:23:45
    val ts = Instant.ofEpochSecond(1596245025)
    val date = LocalDate.from(ts)
    val time = LocalTime.from(ts)
    val dt = LocalDateTime.from(ts)
    val r = Record(BigDecimal(123.456), ts, date, time, dt)
    write("tablerow1.json", t(r))
    // precision > 38
    // write("tablerow1.json", t(r.copy(BigDecimal("1234567890123456789012345678901234567890"))))
    // scale > 9
    // write("tablerow1.json", t(r.copy(BigDecimal("3.14159265358979323846"))))
  }

  case class Record(bd: BigDecimal, ts: Instant, d: LocalDate, t: LocalTime, dt: LocalDateTime)
}

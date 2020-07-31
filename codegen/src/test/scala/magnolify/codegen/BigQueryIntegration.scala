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

import java.{time => jt}

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import magnolify.avro._
import magnolify.bigquery._
import org.joda.{time => jd}

object BigQueryIntegration {
  case class TableRowRecord(numeric: BigDecimal,
                            timestamp: jd.Instant,
                            date: jd.LocalDate,
                            time: jd.LocalTime,
                            datetime: jd.LocalDateTime)

  case class AvroRecord(numeric: BigDecimal,
                        timestamp: jt.Instant,
                        date: jt.LocalDate,
                        time: jt.LocalTime,
                        datetime: jt.LocalDateTime)

  def main(args: Array[String]): Unit = {
    val tr = TableRowRecord(
      BigDecimal(12345),
      jd.Instant.now(),
      jd.LocalDate.now(),
      jd.LocalTime.now(),
      jd.LocalDateTime.now())

    val avro = AvroRecord(
      BigDecimal(12345),
      jt.Instant.now(),
      jt.LocalDate.now(),
      jt.LocalTime.now(),
      jt.LocalDateTime.now()
    )

    val trt = TableRowType[TableRowRecord]

    import LogicalType.BigQuery._
    val at = AvroType[AvroRecord]

    val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

    println("=" * 100)
    println("TableRow")
    println(tr)
    println(mapper.writeValueAsString(trt.schema))
    println(mapper.writeValueAsString(trt(tr)))
    require(tr == trt(trt(tr)))

    println("=" * 100)
    println("Avro")
    println(avro)
    println(at.schema)
    println(at(avro))
    require(avro == at(at(avro)))

    BigQueryClient.client.tabledata()
      .insertAll()
  }
}

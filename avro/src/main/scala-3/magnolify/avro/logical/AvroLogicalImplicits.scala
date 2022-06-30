/*
 * Copyright 2022 Spotify AB
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

package magnolify.avro.logical

import magnolify.avro.AvroField
import org.apache.avro.LogicalTypes.LogicalTypeFactory
import org.apache.avro.{LogicalType, LogicalTypes, Schema}

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.{Instant, LocalDateTime, LocalTime, ZoneOffset}

trait AvroLogicalImplicits

trait AvroTimeMicrosImplicits:
  given afTimestampMicros: AvroField[Instant] = AvroTimeMicros.afTimestampMicros
  given afTimeMicros: AvroField[LocalTime] = AvroTimeMicros.afTimeMicros
  given afLocalTimestampMicros: AvroField[LocalDateTime] = AvroTimeMicros.afLocalTimestampMicros

object AvroTimeMicrosImplicits extends AvroTimeMicrosImplicits

trait AvroTimeMillisImplicits:
  given afTimestampMillis: AvroField[Instant] = AvroTimeMillis.afTimestampMillis
  given afTimeMillis: AvroField[LocalTime] = AvroTimeMillis.afTimeMillis
  given afLocalTimestampMillis: AvroField[LocalDateTime] = AvroTimeMillis.afLocalTimestampMillis

object AvroTimeMillisImplicits extends AvroTimeMillisImplicits

trait AvroBigQueryImplicits:
  given afBigQueryNumeric: AvroField[BigDecimal] = AvroBigQuery.afBigQueryNumeric
  given afBigQueryTimestamp: AvroField[Instant] = AvroBigQuery.afBigQueryTimestamp
  given afBigQueryTime: AvroField[LocalTime] = AvroBigQuery.afBigQueryTime
  given afBigQueryDatetime: AvroField[LocalDateTime] = AvroBigQuery.afBigQueryDatetime

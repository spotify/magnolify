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

import java.time.{Instant, LocalDateTime, LocalTime}

trait AvroLogicalImplicits

trait AvroTimeMicrosImplicits {
  implicit val afTimestampMicros: AvroField[Instant] = AvroTimeMicros.afTimestampMicros
  implicit val afTimeMicros: AvroField[LocalTime] = AvroTimeMicros.afTimeMicros
  implicit val afLocalTimestampMicros: AvroField[LocalDateTime] =
    AvroTimeMicros.afLocalTimestampMicros
}

object AvroTimeMicrosImplicits extends AvroTimeMicrosImplicits

trait AvroTimeMillisImplicits {
  implicit val afTimestampMillis: AvroField[Instant] = AvroTimeMillis.afTimestampMillis
  implicit val afTimeMillis: AvroField[LocalTime] = AvroTimeMillis.afTimeMillis
  implicit val afLocalTimestampMillis: AvroField[LocalDateTime] =
    AvroTimeMillis.afLocalTimestampMillis
}

object AvroTimeMillisImplicits extends AvroTimeMillisImplicits

trait AvroBigQueryImplicits {
  implicit val afBigQueryNumeric: AvroField[BigDecimal] = AvroBigQuery.afBigQueryNumeric
  implicit val afBigQueryTimestamp: AvroField[Instant] = AvroBigQuery.afBigQueryTimestamp
  implicit val afBigQueryTime: AvroField[LocalTime] = AvroBigQuery.afBigQueryTime
  implicit val afBigQueryDatetime: AvroField[LocalDateTime] = AvroBigQuery.afBigQueryDatetime
}

object AvroBigQueryImplicits extends AvroBigQueryImplicits

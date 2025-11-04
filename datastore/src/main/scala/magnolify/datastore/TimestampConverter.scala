/*
 * Copyright 2019 Spotify AB
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

package magnolify.datastore

import java.time.Instant
import com.google.datastore.v1.Value
import com.google.protobuf.Timestamp
import magnolify.shared.Time.{millisFromSecondsAndNanos, millisToSecondsAndNanos}

object TimestampConverter {
  def toInstant(v: Value): Instant = {
    val t = v.getTimestampValue
    Instant.ofEpochMilli(millisFromSecondsAndNanos(t.getSeconds, t.getNanos.toLong))
  }
  def fromInstant(i: Instant): Value.Builder = {
    val (seconds, nanos) = millisToSecondsAndNanos(i.toEpochMilli)
    val t = Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos.toInt)
    Value.newBuilder().setTimestampValue(t)
  }
}

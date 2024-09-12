/*
 * Copyright 2024 Spotify AB
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

import org.apache.hadoop.conf.Configuration

import java.util.Objects

object MagnolifyParquetProperties {
  val WriteGroupedArrays: String = "magnolify.parquet.write-grouped-arrays"
  val WriteGroupedArraysDefault: Boolean = false

  val WriteAvroSchemaToMetadata: String = "magnolify.parquet.write-avro-schema"
  val WriteAvroSchemaToMetadataDefault: Boolean = true

  val ReadTypeKey = "parquet.type.read.type"
  val WriteTypeKey = "parquet.type.write.type"

  // Hash any Configuration values that might affect schema creation to use as part of Schema cache key
  private[parquet] def hashValues(conf: Configuration): Int = {
    Objects.hash(
      Option(conf.get(WriteGroupedArrays)).map(_.toBoolean).getOrElse(WriteGroupedArraysDefault)
    )
  }
}

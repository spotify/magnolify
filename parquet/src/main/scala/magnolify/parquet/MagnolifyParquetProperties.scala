/*
 * Copyright 2025 Spotify AB
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

import magnolify.parquet.ArrayEncoding.Ungrouped

trait MagnolifyParquetProperties extends Serializable {
  def writeArrayEncoding: ArrayEncoding = Ungrouped
  def writeAvroSchemaToMetadata: Boolean = true

  private[parquet] final def schemaUniquenessKey: Int = writeArrayEncoding.hashCode()
}

/**
 * If set in your core-site.xml or an explicit Configruation object passed to ParquetType, will be
 * parsed into MagnolifyParquetProperties
 */
object MagnolifyParquetProperties {
  val Default: MagnolifyParquetProperties = new MagnolifyParquetProperties {}

  val WriteArrayEncoding: String = "magnolify.parquet.write-array-encoding"
  val Ungrouped: String = "ungrouped"
  val OldArrayEncoding: String = "old-array-encoding"
  val NewListEncoding: String = "new-list-encoding"

  val WriteAvroSchemaToMetadata: String = "magnolify.parquet.write-avro-schema"
}

sealed trait ArrayEncoding

object ArrayEncoding {
  case object Ungrouped extends ArrayEncoding
  case object OldArrayEncoding extends ArrayEncoding
  case object NewListEncoding extends ArrayEncoding

  private[magnolify] def parse(str: String): ArrayEncoding = {
    str match {
      case MagnolifyParquetProperties.Ungrouped        => Ungrouped
      case MagnolifyParquetProperties.OldArrayEncoding => OldArrayEncoding
      case MagnolifyParquetProperties.NewListEncoding  => NewListEncoding
      case _ => throw new IllegalStateException(s"Unsupported array encoding $str")
    }
  }
}

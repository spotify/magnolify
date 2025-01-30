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

package magnolify.parquet.logical

import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit

trait TimeTypes {
  protected def unit: TimeUnit
  protected def ts(adjusted: Boolean): LogicalTypeAnnotation =
    LogicalTypeAnnotation.timestampType(adjusted, unit)
  protected def time(adjusted: Boolean): LogicalTypeAnnotation =
    LogicalTypeAnnotation.timeType(adjusted, unit)
}
